package com.example.obo.worker;

import com.example.obo.common.EventMessage;
import com.example.obo.payments.PaymentsServiceGrpc;
import com.example.obo.payments.FinalizeRequest;
import com.example.obo.payments.FinalizeResponse;
import com.nimbusds.jwt.SignedJWT;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentEventConsumer {

    private final JwtDecoder jwtDecoder;
    private final TokenExchangeService tokenExchangeService;
    private final ManagedChannel downstreamChannel;

    public PaymentEventConsumer(JwtDecoder jwtDecoder, TokenExchangeService tokenExchangeService) {
        this.jwtDecoder = jwtDecoder;
        this.tokenExchangeService = tokenExchangeService;
        this.downstreamChannel = ManagedChannelBuilder.forAddress("downstream", 9090)
                .usePlaintext()
                .build();
    }

    @JmsListener(destination = "payment.events")
    public void handlePaymentEvent(EventMessage event) {
        System.out.println("Worker: Received event " + event.getEventType());

        try {
            // Validate event OBO token
            SignedJWT eventJwt = SignedJWT.parse(event.getOboToken());
            Object evtTypeObj = eventJwt.getJWTClaimsSet().getClaim("evt_type");
            String evtType = evtTypeObj != null ? evtTypeObj.toString() : null;
            
            if (!"PAYMENT_INITIATED".equals(evtType)) {
                System.err.println("Worker: Invalid event type: " + evtType);
                return;
            }

            // Decode and set in security context for token exchange
            Jwt jwt = jwtDecoder.decode(event.getOboToken());
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, event.getOboToken(), java.util.Collections.emptyList())
            );

            System.out.println("Worker: Validated event OBO token for " + evtType);

            // Process event payload
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String paymentId = (String) payload.get("payment_id");

            System.out.println("Worker: Processing payment " + paymentId);

            // Exchange event OBO for downstream OBO
            String downstreamOboToken = tokenExchangeService.exchangeFor("downstream-service-c", "payments.finalize");
            System.out.println("Worker: Exchanged for downstream OBO token");

            // Call downstream service
            PaymentsServiceGrpc.PaymentsServiceBlockingStub stub = 
                PaymentsServiceGrpc.newBlockingStub(downstreamChannel)
                    .withInterceptors(new OboGrpcClientInterceptor(() -> downstreamOboToken));

            FinalizeRequest request = FinalizeRequest.newBuilder()
                    .setPaymentId(paymentId)
                    .build();

            FinalizeResponse response = stub.finalizePayment(request);
            System.out.println("Worker: Downstream response: " + response.getStatus() + " - " + response.getMessage());

        } catch (Exception e) {
            System.err.println("Worker: Error processing event: " + e.getMessage());
            e.printStackTrace();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

