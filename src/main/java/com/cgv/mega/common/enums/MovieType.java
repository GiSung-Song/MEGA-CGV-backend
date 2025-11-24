package com.cgv.mega.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MovieType {
    TWO_D("2D", 1.0),
    THREE_D("3D", 1.1)
    ;

    private final String value;
    private final double multiplier;
}
