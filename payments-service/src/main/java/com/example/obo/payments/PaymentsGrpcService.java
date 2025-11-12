package com.example.obo.payments;

import com.example.obo.payments.PaymentsServiceGrpc.PaymentsServiceImplBase;
import com.example.obo.payments.PaymentRequest;
import com.example.obo.payments.PaymentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.security.interceptors.AuthenticatingServerInterceptor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

@GrpcService
public class PaymentsGrpcService extends PaymentsServiceImplBase {

    private final JmsTemplate jmsTemplate;
    private final EventOboTokenService eventOboTokenService;

    public PaymentsGrpcService(JmsTemplate jmsTemplate, EventOboTokenService eventOboTokenService) {
        this.jmsTemplate = jmsTemplate;
        this.eventOboTokenService = eventOboTokenService;
    }

    @Override
    public void initiatePayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        // Extract SecurityContext from gRPC context and set it in Spring SecurityContextHolder
        SecurityContext grpcSecurityContext = AuthenticatingServerInterceptor.SECURITY_CONTEXT_KEY.get();
        if (grpcSecurityContext != null) {
            SecurityContextHolder.setContext(grpcSecurityContext);
        }

        try {
            String paymentId = UUID.randomUUID().toString();
            System.out.println("Payments Service: Processing payment " + paymentId + " for amount " + request.getAmount());

            // Issue event-scoped OBO token
            String eventOboToken = eventOboTokenService.issueEventOboToken("PAYMENT_INITIATED", "payments.process");

            // Publish event with OBO token
            var eventPayload = Map.of(
                "payment_id", paymentId,
                "amount", request.getAmount(),
                "currency", request.getCurrency(),
                "merchant_id", request.getMerchantId()
            );

            var eventMessage = new com.example.obo.common.EventMessage(
                "PAYMENT_INITIATED",
                eventOboToken,
                eventPayload
            );

            jmsTemplate.convertAndSend("payment.events", eventMessage);
            System.out.println("Payments Service: Published PAYMENT_INITIATED event with OBO token");

            PaymentResponse response = PaymentResponse.newBuilder()
                    .setPaymentId(paymentId)
                    .setStatus("INITIATED")
                    .setMessage("Payment initiated successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

