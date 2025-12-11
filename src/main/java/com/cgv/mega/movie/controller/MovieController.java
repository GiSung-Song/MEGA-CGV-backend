package com.cgv.mega.movie.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.movie.dto.MovieInfoResponse;
import com.cgv.mega.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/{movieId}")
    public ResponseEntity<CustomResponse<MovieInfoResponse>> getMovieInfo(@PathVariable("movieId") Long movieId) {
        MovieInfoResponse movieInfo = movieService.getMovieInfo(movieId);

        return ResponseEntity.ok(CustomResponse.of(movieInfo));
    }
}
