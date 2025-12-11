package com.cgv.mega.screening.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AvailableScreeningResponse(
        List<LocalDateTime> availableTime
) {
}