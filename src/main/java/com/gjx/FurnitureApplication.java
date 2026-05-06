package com.gjx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gjx.mapper")
public class FurnitureApplication {
    public static void main(String[] args) {
        SpringApplication.run(FurnitureApplication.class, args);

    }
}
