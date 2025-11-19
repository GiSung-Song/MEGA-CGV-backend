package com.cgv.mega.screening.dto;

import java.time.LocalDateTime;

public record ScreeningTimeDto(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
