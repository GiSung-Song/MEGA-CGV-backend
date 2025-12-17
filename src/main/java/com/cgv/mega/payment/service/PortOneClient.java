package com.cgv.mega.payment.service;

import com.cgv.mega.payment.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${portone.api-url}")
    private String baseUrl;

    @Value("${portone.api-secret}")
    private String apiSecret;

    // 결제 단건 조회 API 호출
    public PortOnePaymentResponse getPaymentInfo(String paymentId) {
        return getClient().get()
                .uri("/payments/{paymentId}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret)
                .retrieve()
                .bodyToMono(PortOnePaymentResponse.class)
                .block();
    }

    // 환불 API 호출
    public RefundResult refundPayment(String paymentId, PortOneCancelRequest request) {
        PortOneCancellationWrapper response = getClient().post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PortOneCancellationWrapper.class)
                .block();

        PortOneCancellationResponse cancellation = response.cancellation();

        return toRefundResult(cancellation);
    }

    private WebClient getClient() {
        return webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    private RefundResult toRefundResult(PortOneCancellationResponse p) {
        if (p == null) {
            return RefundResult.failure("EMPTY_RESPONSE");
        }

        System.out.println("[PORTONE CANCEL RESPONSE]");
        System.out.println("status = " + p.status());
        System.out.println("totalAmount = " + p.totalAmount());
        System.out.println("reason = " + p.reason());

        if (!"SUCCEEDED".equalsIgnoreCase(p.status())) {
            return RefundResult.failure("CANCEL_NOT_SUCCEEDED: " + p.status());
        }

        if (p.totalAmount() == null || p.totalAmount() <= 0) {
            return RefundResult.failure("INVALID_TOTAL_AMOUNT");
        }

        return RefundResult.success(BigDecimal.valueOf(p.totalAmount()));
    }
}
