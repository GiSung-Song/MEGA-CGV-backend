package com.cgv.mega.payment.dto;

import java.math.BigDecimal;

public record PortOneCancelRequest(
        BigDecimal amount,
        String reason
) {
}
