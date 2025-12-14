package com.scccy.aimodel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.scccy.aimodel.dao.mapper")
public class AiModelCallApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiModelCallApplication.class, args);
    }

}
