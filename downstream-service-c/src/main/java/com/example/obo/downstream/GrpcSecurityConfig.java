package com.example.obo.downstream;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
public class GrpcSecurityConfig {

    @Bean
    @GrpcGlobalServerInterceptor
    public ServerInterceptor grpcServerInterceptor(JwtDecoder jwtDecoder) {
        return new GrpcServerInterceptor(jwtDecoder);
    }
}

