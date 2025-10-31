package com.cgv.mega.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MovieType {
    TWO_D("2D"),
    THREE_D("3D")
    ;

    private final String value;
}
