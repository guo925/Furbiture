package com.gjx.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter.TtlFunction;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 配置
 * 配置序列化方式与缓存管理器
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /** 商品列表缓存的基础 TTL，实际过期时间在此基础上叠加随机抖动。 */
    private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(5);

    /** TTL 抖动上限的分母：实际 TTL = 基础值 + [0, 基础值 / 该值)。 */
    private static final int TTL_JITTER_DIVISOR = 10;

    /**
     * 构建缓存序列化用的 ObjectMapper
     * <p>
     * 缓存值需要保留类型信息才能还原为原对象，但 {@code LaissezFaireSubTypeValidator}
     * 会放行任意类，一旦 Redis 被写入恶意数据即可触发反序列化 gadget 链（RCE 风险）。
     * 这里改为白名单校验器：仅允许本项目的实体/DTO 与必要的 JDK 类型被反序列化。
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JavaTimeModule()); // 支持 LocalDateTime 序列化

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.gjx.")
                // 商品列表缓存的是 Page<Product>，其运行时类型为 MyBatis-Plus 的分页类，
                // 不属于本项目包名。这里精确放行该具体类而非整个 com.baomidou 前缀——
                // 前缀放行会把 MyBatis-Plus 全部类型纳入反序列化范围，收窄到单个类风险最小。
                .allowIfSubType("com.baomidou.mybatisplus.extension.plugins.pagination.Page")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.lang.")
                .build();
        mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jacksonSerializer.setObjectMapper(createObjectMapper());

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(createObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        java.util.Map<String, RedisCacheConfiguration> cacheConfigs = new java.util.HashMap<>();
        cacheConfigs.put("categoryTree", defaultConfig.entryTtl(Duration.ofHours(2)));
        // productList 的 TTL 叠加随机抖动：固定 TTL 会让同一批 key 在同一秒集体过期，
        // 随后的请求洪峰同时回源数据库（缓存雪崩）。抖动把过期时刻打散，削掉这个尖峰。
        cacheConfigs.put("productList", defaultConfig.entryTtl(jitteredTtl(PRODUCT_LIST_TTL)));
        // 曾经在此注册过 "productDetail"（TTL 10 分钟），但全项目没有任何
        // @Cacheable(value = "productDetail")，属于死配置（从未生效），故删除。
        // 若将来要启用商品详情缓存，必须同时补上对应的 @Cacheable 与 @CacheEvict，
        // 否则“只写不清”同样会导致数据陈旧。

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * 构造带随机抖动的 TTL 策略：每次写入返回 {@code 基础TTL + [0, 基础TTL/10)} 的随机毫秒数。
     * <p>
     * 这里使用 Spring Data Redis 3.2+ 官方提供的 {@link TtlFunction}
     * （{@code RedisCacheConfiguration#entryTtl(TtlFunction)}），无需自行包装 {@code RedisCacheWriter}，
     * 也无需任何反射/代理——{@code RedisCache} 会在每次 put 时调用它计算实际 TTL。
     *
     * @param baseTtl 基础过期时间
     * @return 带抖动的 TTL 计算函数
     */
    private static TtlFunction jitteredTtl(Duration baseTtl) {
        long baseMillis = baseTtl.toMillis();
        // 抖动上限取基础值的 1/10；至少保留 1ms 的取值范围，避免基础 TTL 过小时抖动恒为 0
        long jitterRange = Math.max(1L, baseMillis / TTL_JITTER_DIVISOR + 1L);
        return (key, value) -> Duration.ofMillis(baseMillis + ThreadLocalRandom.current().nextLong(jitterRange));
    }
}
