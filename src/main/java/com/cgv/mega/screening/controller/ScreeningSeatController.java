package com.cgv.mega.screening.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.common.security.CustomUserDetails;
import com.cgv.mega.screening.dto.ScreeningSeatHoldDto;
import com.cgv.mega.screening.service.ScreeningSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/screenings")
public class ScreeningSeatController {

    private final ScreeningSeatService screeningSeatService;

    @PostMapping("{screeningId}/seats/hold")
    public ResponseEntity<CustomResponse<Void>> holdScreeningSeat(
            @PathVariable("screeningId") Long screeningId,
            @RequestBody @Valid ScreeningSeatHoldDto req,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        screeningSeatService.holdScreeningSeat(details.id(), screeningId, req);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @DeleteMapping("{screeningId}/seats/hold")
    public ResponseEntity<CustomResponse<Void>> cancelHoldScreeningSeat(
            @PathVariable("screeningId") Long screeningId,
            @RequestBody @Valid ScreeningSeatHoldDto req,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        screeningSeatService.cancelHoldScreeningSeat(details.id(), req);

        return ResponseEntity.ok(CustomResponse.of());
    }
}
