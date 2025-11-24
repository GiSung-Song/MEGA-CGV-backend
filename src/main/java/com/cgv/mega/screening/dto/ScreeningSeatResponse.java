package com.cgv.mega.screening.dto;

import com.cgv.mega.common.enums.SeatType;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;

import java.util.List;

public record ScreeningSeatResponse(
        Long screeningId,
        int basePrice,
        List<ScreeningSeatInfo> screeningSeatInfos

) {
    public record ScreeningSeatInfo(
            Long screeningSeatId,
            String rowLabel,
            int colNumber,
            SeatType seatType,
            ScreeningSeatStatus status
    ) {
    }
}