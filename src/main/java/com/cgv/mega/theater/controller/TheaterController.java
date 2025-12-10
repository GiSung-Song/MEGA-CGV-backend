package com.cgv.mega.theater.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.theater.dto.TheaterListResponse;
import com.cgv.mega.theater.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theaters")
public class TheaterController {

    private final TheaterService theaterService;

    @GetMapping
    public ResponseEntity<CustomResponse<TheaterListResponse>> getAllTheaters() {
        TheaterListResponse allTheaters = theaterService.getAllTheaterInfo();

        return ResponseEntity.ok(CustomResponse.of(allTheaters));
    }
}
