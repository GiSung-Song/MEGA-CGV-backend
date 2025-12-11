package com.cgv.mega.reservation.dto;

import com.cgv.mega.reservation.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationDetailResponse(
        Long movieId,
        String title,
        String movieType,
        String posterUrl,
        int duration,

        Long screeningId,
        LocalDateTime startTime,
        LocalDateTime endTime,

        Long theaterId,
        String theaterName,
        String theaterType,

        List<SeatInfo> seatInfos,

        Long reservationGroupId,
        String reservationStatus,
        LocalDateTime reservationCreatedAt,
        LocalDateTime reservationCancelledAt,

        String paymentStatus,
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
            String seatNumber,
            String seatType
    ) { }

    public static ReservationDetailResponse from(ReservationDetailDto dto) {
        return new ReservationDetailResponse(
                dto.movieId(),
                dto.title(),
                dto.movieType().getValue(),
                dto.posterUrl(),
                dto.duration(),

                dto.screeningId(),
                dto.startTime(),
                dto.endTime().minusMinutes(10),

                dto.theaterId(),
                dto.theaterName(),
                dto.theaterType().getValue(),

                dto.seatInfos().stream()
                        .map(seat -> new SeatInfo(
                                seat.rowLabel() + seat.colNumber(),
                                seat.seatType().getKorean()
                        ))
                        .toList(),

                dto.reservationGroupId(),
                dto.reservationStatus().getKorean(),
                dto.reservationCreatedAt(),
                dto.reservationStatus() == ReservationStatus.CANCELLED ? dto.reservationCancelledAt() : null,

                dto.paymentStatus().getKorean(),
                dto.paymentMethod(),
                dto.paymentAmount(),
                dto.refundAmount(),
                dto.merchantUid(),
                dto.paymentId(),

                dto.buyerName(),
                dto.buyerPhoneNumber(),
                dto.buyerEmail()
        );
    }
}
