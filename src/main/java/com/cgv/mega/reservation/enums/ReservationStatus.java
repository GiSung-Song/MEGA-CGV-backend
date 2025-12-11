package com.cgv.mega.reservation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    PENDING("결제 대기"),
    PAID("결제 완료"),
    CANCELLED("예약 취소")
    ;

    private final String korean;
}
