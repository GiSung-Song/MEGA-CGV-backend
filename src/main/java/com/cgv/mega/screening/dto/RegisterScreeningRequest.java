package com.cgv.mega.screening.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RegisterScreeningRequest(
        @NotNull(message = "영화 식별자 ID는 필수입니다.")
        Long movieId,

        @NotNull(message = "상영관 식별자 ID는 필수입니다.")
        Long theaterId,

        @NotNull(message = "상영 시작 시간은 필수입니다.")
        LocalDateTime startTime
) {
}