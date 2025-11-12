package com.example.obo.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;

@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class})
public class WorkerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerServiceApplication.class, args);
    }
}

