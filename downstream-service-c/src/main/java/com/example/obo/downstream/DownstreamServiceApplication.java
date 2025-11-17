package com.example.obo.downstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {OAuth2ResourceServerAutoConfiguration.class})
@ComponentScan(basePackages = {"com.example.obo.downstream", "com.example.obo.common"})
public class DownstreamServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DownstreamServiceApplication.class, args);
    }
}

