package com.cgv.mega.theater.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TheaterType {
    TWO_D("2D관", 1.0),
    FOUR_DX("4DX관", 1.2),
    IMAX("IMAX관", 1.3),
    SCREEN_X("SCREEN X관", 1.5)
    ;

    private final String value;
    private final double multiplier;
}
