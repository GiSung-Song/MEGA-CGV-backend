package com.cgv.mega.screening.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.screening.dto.MovieScreeningResponse;
import com.cgv.mega.screening.dto.ScreeningDateMovieResponse;
import com.cgv.mega.screening.dto.ScreeningSeatResponse;
import com.cgv.mega.screening.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/screenings")
public class ScreeningController {

    private final ScreeningService screeningService;

    @GetMapping("/movies")
    public ResponseEntity<CustomResponse<ScreeningDateMovieResponse>> getScreeningMovies(
            @RequestParam LocalDate date
    ) {
        ScreeningDateMovieResponse response = screeningService.getScreeningMovies(date);

        return ResponseEntity.ok(CustomResponse.of(response));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<CustomResponse<MovieScreeningResponse>> getMovieScreenings(
            @PathVariable("movieId") Long movieId,
            @RequestParam LocalDate date
    ) {
        MovieScreeningResponse response = screeningService.getMovieScreenings(movieId, date);

        return ResponseEntity.ok(CustomResponse.of(response));
    }

    @GetMapping("/{screeningId}/seats")
    public ResponseEntity<CustomResponse<ScreeningSeatResponse>> getScreeningSeats(
            @PathVariable("screeningId") Long screeningId
    ) {
        ScreeningSeatResponse response = screeningService.getScreeningSeatStatus(screeningId);

        return ResponseEntity.ok(CustomResponse.of(response));
    }
}