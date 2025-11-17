package com.example.obo.payments;

import com.example.obo.common.GrpcServerInterceptor;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
public class GrpcSecurityConfig {

    @Bean
    @GrpcGlobalServerInterceptor
    public ServerInterceptor grpcServerInterceptor(
            JwtDecoder jwtDecoder,
            @Value("${spring.application.name}") String expectedAudience) {
        return new GrpcServerInterceptor(jwtDecoder, expectedAudience);
    }
}
