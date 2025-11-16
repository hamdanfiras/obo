package com.example.obo.downstream;

import com.example.obo.payments.PaymentsServiceGrpc.PaymentsServiceImplBase;
import com.example.obo.payments.FinalizeRequest;
import com.example.obo.payments.FinalizeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class DownstreamGrpcService extends PaymentsServiceImplBase {

    @Override
    public void finalizePayment(FinalizeRequest request, StreamObserver<FinalizeResponse> responseObserver) {
        String paymentId = request.getPaymentId();
        System.out.println("Downstream Service C: Finalizing payment " + paymentId);

        // In a real implementation, this would perform the actual finalization
        // For PoC, we just log and return success

        FinalizeResponse response = FinalizeResponse.newBuilder()
                .setStatus("FINALIZED")
                .setMessage("Payment " + paymentId + " finalized successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

