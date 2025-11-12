package com.example.obo.downstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;

@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class})
public class DownstreamServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DownstreamServiceApplication.class, args);
    }
}

