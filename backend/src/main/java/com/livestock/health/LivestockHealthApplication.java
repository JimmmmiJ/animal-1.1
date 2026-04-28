package com.livestock.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 畜牧健康监测与预警管理平台 - 启动类
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class LivestockHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(LivestockHealthApplication.class, args);
    }
}
