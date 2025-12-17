package com.cgv.mega.payment.dto;

import java.math.BigDecimal;

public record PortOnePaymentResponse(
        String id,
        String orderId,
        String status,
        Amount amount,
        Method method,
        String statusChangedAt,
        Failure failure,
        Cancellation cancellation
) {
    public record Amount(
            BigDecimal total,
            BigDecimal cancelled
    ) {}

    public record Method(
            String type,
            String provider,
            String cardName,
            Integer cardQuota
    ) {}

    public record Failure(
            String reason
    ) {}

    public record Cancellation(
            String reason,
            String cancelledAt
    ) {}
}