package com.cgv.mega.reservation.dto;

import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.reservation.enums.ReservationStatus;
import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.theater.enums.TheaterType;

import java.time.LocalDateTime;
import java.util.List;

public record ReservationListDto(
        Long reservationGroupId,
        String title,
        MovieType movieType,

        LocalDateTime startTime,
        String theaterName,
        TheaterType theaterType,

        List<SeatDto> seatDtoList,

        ReservationStatus status,

        int totalPrice,
        String posterUrl,
        LocalDateTime updatedAt
) {
    public record SeatDto(
            String rowLabel,
            int colNumber,
            SeatType seatType
    ) { }
}
