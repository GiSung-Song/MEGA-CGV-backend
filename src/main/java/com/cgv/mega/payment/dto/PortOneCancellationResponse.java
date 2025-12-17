package com.cgv.mega.payment.dto;

import java.time.OffsetDateTime;

public record PortOneCancellationResponse(
        String status,
        String id,
        String pgCancellationId,
        Long totalAmount,
        Long taxFreeAmount,
        Long vatAmount,
        Long easyPayDiscountAmount,
        String reason,
        OffsetDateTime requestedAt,
        OffsetDateTime cancelledAt
) {
}
