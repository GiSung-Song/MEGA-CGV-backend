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
        PortOneCancelResponse response = getClient().post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PortOneCancelResponse.class)
                .block();

        return toRefundResult(response);
    }

    private WebClient getClient() {
        return webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    private RefundResult toRefundResult(PortOneCancelResponse response) {
        if (response == null || response.payment() == null) {
            return RefundResult.failure("EMPTY_RESPONSE");
        }
        PortOnePaymentResponse.Payment p = response.payment();

        if (p.failure() != null && p.failure().reason() != null) {
            return RefundResult.failure(p.failure().reason());
        }

        if (p.cancellation() == null || p.amount() == null || p.amount().cancelled() == null) {
            return RefundResult.failure("CANCELLATION_OR_AMOUNT_MISSING");
        }

        String status = p.status();
        BigDecimal cancelledAmount = p.amount().cancelled();

        boolean success = ("CANCELLED".equalsIgnoreCase(status)
                || "PARTIAL_CANCELLED".equalsIgnoreCase(status))
                && cancelledAmount.compareTo(BigDecimal.ZERO) > 0;

        if (!success) {
            return RefundResult.failure("INVALID_STATUS_OR_AMOUNT: " + status);
        }

        return RefundResult.success(cancelledAmount);
    }
}
