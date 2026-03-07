package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// @SpringBootApplication ：这是一个 Spring Boot 应用的注解，它包含了 @Configuration、@EnableAutoConfiguration 和 @ComponentScan 等注解的功能。
@SpringBootApplication
@EnableTransactionManagement // Spring 事务管理的注解，它开启了注解方式的事务管理。
@Slf4j
@EnableCaching // 缓存注解功能
@EnableScheduling   // 开启任务调度
public class SkyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
    }
}
