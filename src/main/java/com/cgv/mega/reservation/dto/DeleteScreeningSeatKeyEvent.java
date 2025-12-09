package com.cgv.mega.reservation.dto;

import java.util.List;

public record DeleteScreeningSeatKeyEvent(
        List<String> keys
) {
}
