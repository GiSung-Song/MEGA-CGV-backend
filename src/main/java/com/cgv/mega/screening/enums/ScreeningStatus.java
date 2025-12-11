package com.cgv.mega.screening.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScreeningStatus {
    SCHEDULED("상영 예정"),
    CANCELED("상영 취소"),
    ENDED("상영 종료")
    ;

    @JsonValue
    private final String value;
}