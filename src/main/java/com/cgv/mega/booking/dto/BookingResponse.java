package com.cgv.mega.booking.dto;

import java.math.BigDecimal;

public record BookingResponse(
        Long reservationGroupId,
        String merchantUid,
        BigDecimal expectedAmount
) {
}
