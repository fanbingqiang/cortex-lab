package com.cortex;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.cortex.mapper", "com.cortex.lab.mapper"})
public class CortexApplication {
    public static void main(String[] args) {
        SpringApplication.run(CortexApplication.class, args);
    }
}
