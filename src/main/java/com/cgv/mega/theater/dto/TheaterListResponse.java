package com.cgv.mega.theater.dto;

import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.theater.enums.TheaterType;

import java.util.List;
import java.util.Map;

public record TheaterListResponse(
    List<TheaterInfo> theaterInfoList
) {
    public record TheaterInfo(
            Long theaterId,
            String theaterName,
            TheaterType theaterType,
            int totalSeat,
            Map<SeatType, Integer> seatCount
    ) {}
}