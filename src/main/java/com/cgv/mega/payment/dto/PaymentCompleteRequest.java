package com.cgv.mega.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentCompleteRequest(
        @NotBlank(message = "거래 번호는 필수입니다.")
        String paymentId,

        @NotNull(message = "거래 금액은 필수입니다.")
        BigDecimal amount,

        @NotNull(message = "예약 그룹 ID는 필수입니다.")
        Long reservationGroupId
) {
}
