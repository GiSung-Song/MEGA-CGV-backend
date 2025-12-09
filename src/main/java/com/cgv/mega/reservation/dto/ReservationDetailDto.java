package com.cgv.mega.reservation.dto;

import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.payment.enums.PaymentStatus;
import com.cgv.mega.reservation.enums.ReservationStatus;
import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.theater.enums.TheaterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationDetailDto(
        Long movieId,
        String title,
        MovieType movieType,
        String posterUrl,
        int duration,

        Long screeningId,
        LocalDateTime startTime,
        LocalDateTime endTime,

        Long theaterId,
        String theaterName,
        TheaterType theaterType,

        List<SeatInfo> seatInfos,

        Long reservationGroupId,
        ReservationStatus reservationStatus,
        LocalDateTime reservationCreatedAt,
        LocalDateTime reservationCancelledAt,

        PaymentStatus paymentStatus,
        String paymentMethod,
        BigDecimal paymentAmount,
        BigDecimal refundAmount,
        String merchantUid,
        String paymentId,

        String buyerName,
        String buyerPhoneNumber,
        String buyerEmail
) {
    public record SeatInfo(
            String rowLabel,
            int colNumber,
            SeatType seatType
    ) { }
}
