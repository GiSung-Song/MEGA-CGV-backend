package com.cgv.mega.screening.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.screening.dto.AvailableScreeningResponse;
import com.cgv.mega.screening.dto.MovieScreeningForAdminResponse;
import com.cgv.mega.screening.dto.RegisterScreeningRequest;
import com.cgv.mega.screening.service.ScreeningSeatService;
import com.cgv.mega.screening.service.ScreeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/screenings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScreeningController {

    private final ScreeningService screeningService;
    private final ScreeningSeatService screeningSeatService;

    @GetMapping
    public ResponseEntity<CustomResponse<AvailableScreeningResponse>> getAvailableScreeningTime(
            @RequestParam Long movieId,
            @RequestParam Long theaterId,
            @RequestParam LocalDate date
    ) {
        AvailableScreeningResponse response = screeningService.getAvailableScreeningTime(movieId, theaterId, date);

        return ResponseEntity.ok(CustomResponse.of(response));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Void>> registerScreening(
            @RequestBody @Valid RegisterScreeningRequest request
    ) {
        screeningService.registerScreening(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.of(HttpStatus.CREATED));
    }

    @DeleteMapping("/{screeningId}")
    public ResponseEntity<CustomResponse<Void>> cancelScreening(
            @PathVariable("screeningId") Long screeningId
    ) {
        screeningService.cancelScreening(screeningId);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<CustomResponse<MovieScreeningForAdminResponse>> getMovieScreenings(
            @PathVariable("movieId") Long moveId
    ) {
        MovieScreeningForAdminResponse response = screeningService.getMovieScreeningsForAdmin(moveId);

        return ResponseEntity.ok(CustomResponse.of(response));
    }

    @PatchMapping("/seats/{screeningSeatId}/fix")
    public ResponseEntity<CustomResponse<Void>> fixSeat(
            @PathVariable("screeningSeatId") Long screeningSeatId
    ) {
        screeningSeatService.fixingScreeningSeat(screeningSeatId);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @PatchMapping("/seats/{screeningSeatId}/restore")
    public ResponseEntity<CustomResponse<Void>> restoreSeat(
            @PathVariable("screeningSeatId") Long screeningSeatId
    ) {
        screeningSeatService.restoringScreeningSeat(screeningSeatId);

        return ResponseEntity.ok(CustomResponse.of());
    }
}
