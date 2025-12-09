package com.cgv.mega.seat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatType {
    NORMAL("일반", 1.0),
    PREMIUM("프리미엄", 1.3),
    ROOM("방", 1.5)
    ;

    private final String korean;
    private final double multiplier;
}