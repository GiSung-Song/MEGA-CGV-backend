package com.cgv.mega.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    READY("결제 대기"),
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),
    FAILED_VERIFICATION("결제 검증 실패"),
    CANCELLED("결제 취소")
    ;

    private final String korean;
}
