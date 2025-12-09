package com.cgv.mega.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReservationListResponse(
        Long reservationGroupId,
        String title,
        String movieType,

        LocalDateTime startTime,
        String theaterName,
        String theaterType,

        List<SeatInfo> seats,

        String reservationStatus,

        int totalPrice,
        String posterUrl,
        LocalDateTime updatedAt
) {
    public record SeatInfo(
            String seatNumber,
            String seatType
    ) {
    }

    public static ReservationListResponse from(ReservationListDto dto) {
        return new ReservationListResponse(
                dto.reservationGroupId(),
                dto.title(),
                dto.movieType().getValue(),

                dto.startTime(),
                dto.theaterName(),
                dto.theaterType().getValue(),

                dto.seatDtoList().stream()
                        .map(seat -> new SeatInfo(
                                seat.rowLabel() + seat.colNumber(), seat.seatType().getKorean()))
                        .toList(),

                dto.status().getKorean(),

                dto.totalPrice(),
                dto.posterUrl(),
                dto.updatedAt()
        );
    }
}
