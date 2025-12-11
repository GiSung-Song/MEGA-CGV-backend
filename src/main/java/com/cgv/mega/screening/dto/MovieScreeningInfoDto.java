package com.cgv.mega.screening.dto;

import com.cgv.mega.screening.enums.ScreeningStatus;

import java.time.LocalDateTime;

public record MovieScreeningInfoDto(
        Long screeningId,
        Long theaterId,
        String theaterName,
        Long remainSeat,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int sequence,
        ScreeningStatus screeningStatus
) {
}