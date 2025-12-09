package com.cgv.mega.screening.dto;

import com.cgv.mega.screening.enums.DisplayScreeningSeatStatus;
import com.cgv.mega.seat.enums.SeatType;

import java.util.List;

public record ScreeningSeatResponse(
        Long screeningId,
        List<ScreeningSeatInfo> screeningSeatInfos

) {
    public record ScreeningSeatInfo(
            Long screeningSeatId,
            String rowLabel,
            int colNumber,
            SeatType seatType,
            DisplayScreeningSeatStatus status,
            int price
    ) {
    }
}