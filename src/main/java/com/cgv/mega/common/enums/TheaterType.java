package com.cgv.mega.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TheaterType {
    TWO_D("2D관"),
    FOUR_DX("4DX관"),
    IMAX("IMAX관"),
    SCREEN_X("SCREEN X관")
    ;

    private final String value;
}
