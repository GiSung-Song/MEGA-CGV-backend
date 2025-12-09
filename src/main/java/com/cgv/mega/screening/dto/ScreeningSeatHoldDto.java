package com.cgv.mega.screening.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record ScreeningSeatHoldDto(
        @NotEmpty(message = "최소 한 좌석은 필수입니다.")
        Set<Long> screeningSeatIds
) {
}