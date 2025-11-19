package com.cgv.mega.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatType {
    NORMAL("일반"),
    PREMIUM("프리미엄"),
    ROOM("방")
    ;

    private final String korean;
}