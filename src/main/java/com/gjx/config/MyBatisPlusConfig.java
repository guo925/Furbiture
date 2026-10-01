package com.gjx.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 配置分页插件
     */
    /**
     * 单页最大条数上限。
     * <p>
     * 所有列表接口的 size 均直接来自 {@code @RequestParam}，不限上限时
     * 一个 {@code size=100000} 的请求即可把整表（含 product.description 这类 TEXT 列）
     * 拉进内存，属于零成本的 DoS。这里设全局硬上限，超限由插件自动截断为 100。
     */
    private static final long MAX_PAGE_SIZE = 100L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，指定数据库类型为 MySQL
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 超过 MAX_PAGE_SIZE 的 size 会被自动收敛，避免单请求拖垮内存
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
