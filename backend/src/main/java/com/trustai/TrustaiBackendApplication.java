package com.trustai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.flowable.spring.boot.EnableFlowableProcessEngine;

@SpringBootApplication
@EnableFeignClients
@EnableFlowableProcessEngine   // 添加这一行
public class TrustaiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrustaiBackendApplication.class, args);
    }
     }