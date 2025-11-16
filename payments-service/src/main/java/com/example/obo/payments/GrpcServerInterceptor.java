package com.example.obo.payments;

import io.grpc.*;
import io.grpc.Context;
import io.grpc.Contexts;
import net.devh.boot.grpc.server.security.interceptors.AuthenticatingServerInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.stream.Collectors;

public class GrpcServerInterceptor implements ServerInterceptor {

    private final JwtDecoder jwtDecoder;

    public GrpcServerInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"),
                    new Metadata());
            return new ServerCall.Listener<ReqT>() {
            };
        }

        String token = authHeader.substring(7);
        try {
            Jwt jwt = jwtDecoder.decode(token);

            // Validate audience
            String audience = jwt.getAudience() != null && !jwt.getAudience().isEmpty()
                    ? jwt.getAudience().get(0)
                    : null;
            if (!"payments-service".equals(audience)) {
                call.close(Status.PERMISSION_DENIED.withDescription("Invalid audience"), new Metadata());
                return new ServerCall.Listener<ReqT>() {
                };
            }

            // Extract authorities from scope
            String scope = jwt.getClaimAsString("scope");
            List<SimpleGrantedAuthority> authorities = scope != null
                    ? java.util.Arrays.stream(scope.split(" "))
                            .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                            .collect(Collectors.toList())
                    : java.util.Collections.emptyList();

            Authentication auth = new UsernamePasswordAuthenticationToken(jwt, token, authorities);
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(auth);
            return Contexts.interceptCall(
                    Context.current().withValue(AuthenticatingServerInterceptor.SECURITY_CONTEXT_KEY, securityContext),
                    call, headers, next);
        } catch (Exception e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token: " + e.getMessage()), new Metadata());
            return new ServerCall.Listener<ReqT>() {
            };
        }
    }
}
