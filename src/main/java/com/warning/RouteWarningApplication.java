package com.warning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.warning.mapper")
public class RouteWarningApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteWarningApplication.class, args);
    }
}
