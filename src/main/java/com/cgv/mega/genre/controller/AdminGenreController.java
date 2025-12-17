package com.cgv.mega.genre.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.genre.dto.GenreListResponse;
import com.cgv.mega.genre.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/genres")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenreController {
    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<CustomResponse<GenreListResponse>> getAllGenres() {
        GenreListResponse response = genreService.getAllGenre();

        return ResponseEntity.ok(CustomResponse.of(response));
    }
}
