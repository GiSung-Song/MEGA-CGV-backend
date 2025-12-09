package com.cgv.mega.screening.dto;

import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;

public record ScreeningSeatDto(
        Long screeningSeatId,
        String rowLabel,
        int colNumber,
        SeatType seatType,
        ScreeningSeatStatus status,
        int price
) {
}
