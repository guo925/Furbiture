package com.gjx.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败次数限制服务
 * <p>
 * <b>为什么需要它</b>：登录接口如果不限制失败次数，攻击者可以用字典无限次尝试密码
 * （撞库 / 暴力破解）。BCrypt 只能拖慢单次验证，无法阻止"试一百万次"。
 * 企业里的标准做法是两层防护：失败计数 + 临时锁定（本类负责），
 * 必要时再叠加图形验证码 / 风控。
 * <p>
 * <b>为什么用 Redis 而不是本地 Map</b>：应用一旦多实例部署（水平扩容），
 * 本地 Map 的计数各存各的，攻击者轮询实例即可绕过限制。
 * Redis 是共享存储，所有实例读到同一份计数。
 * <p>
 * <b>容错取舍</b>：Redis 不可用时本服务选择"放行"（fail-open）并打告警日志。
 * 原因是登录限流属于纵深防御的一层，若 Redis 故障时 fail-close，
 * 会导致全站用户无法登录——可用性损失远大于限流失效的风险。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /**
     * 允许的最大连续失败次数，达到即锁定
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * 锁定时长（分钟）
     */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /**
     * Redis key 前缀，便于按前缀检索与清理
     */
    private static final String KEY_PREFIX = "login:fail:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断账号是否处于锁定期
     *
     * @param username 用户名
     * @return true 表示已锁定，应拒绝登录
     */
    public boolean isBlocked(String username) {
        try {
            String value = stringRedisTemplate.opsForValue().get(buildKey(username));
            return value != null && Integer.parseInt(value) >= MAX_FAILED_ATTEMPTS;
        } catch (Exception e) {
            // 计数读取失败不应阻断正常登录，记录告警即可
            log.warn("[登录限流] 读取失败次数异常，本次放行 username={}", username, e);
            return false;
        }
    }

    /**
     * 记录一次登录失败，并在首次失败时设置过期时间
     * <p>
     * 过期时间让计数自动"冷却"：用户停止尝试 15 分钟后计数归零，
     * 避免被误锁的账号需要人工解锁。
     *
     * @param username 用户名
     */
    public void recordFailure(String username) {
        try {
            String key = buildKey(username);
            Long attempts = stringRedisTemplate.opsForValue().increment(key);
            if (attempts == null) {
                return;
            }
            if (attempts == 1L) {
                stringRedisTemplate.expire(key, LOCK_DURATION);
            }
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                log.warn("[登录限流] 账号已被锁定 username={}, 失败次数={}", username, attempts);
            }
        } catch (Exception e) {
            log.warn("[登录限流] 记录失败次数异常 username={}", username, e);
        }
    }

    /**
     * 登录成功后清空失败计数
     *
     * @param username 用户名
     */
    public void clear(String username) {
        try {
            stringRedisTemplate.delete(buildKey(username));
        } catch (Exception e) {
            log.warn("[登录限流] 清空失败次数异常 username={}", username, e);
        }
    }

    private String buildKey(String username) {
        return KEY_PREFIX + username;
    }
}
