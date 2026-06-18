package com.moli;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@MapperScan(basePackages = "com.moli.common.dao.mapper")
@SpringBootApplication
public class LanggraphApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LanggraphApiApplication.class, args);
    }

}
