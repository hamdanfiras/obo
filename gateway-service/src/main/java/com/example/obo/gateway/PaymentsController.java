package com.example.obo.gateway;

import com.example.obo.payments.PaymentsServiceGrpc;
import com.example.obo.payments.PaymentRequest;
import com.example.obo.payments.PaymentResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentsController {

    private final TokenExchangeService tokenExchangeService;
    private final ManagedChannel paymentsChannel;

    public PaymentsController(
            TokenExchangeService tokenExchangeService,
            @Value("${grpc.client.payments-service.address}") String paymentsAddress) {
        this.tokenExchangeService = tokenExchangeService;
        String[] parts = paymentsAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        this.paymentsChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('SCOPE_payments.initiate')")
    public Map<String, Object> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String userId = jwt.getSubject();
        System.out.println("Gateway: User " + userId + " initiating payment");

        // Exchange for OBO token
        String oboToken = tokenExchangeService.exchangeFor("payments-service", "payments.initiate");
        System.out.println("Gateway: Exchanged for OBO token");

        PaymentsServiceGrpc.PaymentsServiceBlockingStub stub = PaymentsServiceGrpc.newBlockingStub(paymentsChannel)
                .withInterceptors(new OboGrpcClientInterceptor(() -> oboToken));

        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setAmount(request.getOrDefault("amount", "100.00"))
                .setCurrency(request.getOrDefault("currency", "USD"))
                .setMerchantId(request.getOrDefault("merchant_id", "merchant-123"))
                .build();

        PaymentResponse response = stub.initiatePayment(grpcRequest);

        return Map.of(
                "payment_id", response.getPaymentId(),
                "status", response.getStatus(),
                "message", response.getMessage());
    }
}
