package com.cgv.mega.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ReservationRequest(
        @NotEmpty(message = "최소 한 좌석은 필수입니다.")
        Set<Long> screeningSeatIds
) {
}