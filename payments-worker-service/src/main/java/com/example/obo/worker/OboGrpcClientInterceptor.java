package com.example.obo.worker;

import io.grpc.*;

import java.util.function.Supplier;

public class OboGrpcClientInterceptor implements ClientInterceptor {

    private final Supplier<String> tokenSupplier;

    public OboGrpcClientInterceptor(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Metadata.Key<String> authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                headers.put(authKey, "Bearer " + tokenSupplier.get());
                super.start(responseListener, headers);
            }
        };
    }
}

