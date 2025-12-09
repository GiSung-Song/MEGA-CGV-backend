package com.cgv.mega.payment.dto;

import java.math.BigDecimal;

public record RefundResult(
        boolean success,
        BigDecimal cancelledAmount,
        String reason
) {
    public static RefundResult success(BigDecimal cancelledAmount) {
        return new RefundResult(true, cancelledAmount, null);
    }
    public static RefundResult failure(String reason) {
        return new RefundResult(false, BigDecimal.ZERO, reason);
    }
    public boolean isFailure() { return !success; }
}
