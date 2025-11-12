package com.cgv.mega.movie.controller;

import co.elastic.clients.elasticsearch.ml.Page;
import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.movie.dto.MovieListResponse;
import com.cgv.mega.movie.dto.RegisterMovieRequest;
import com.cgv.mega.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<CustomResponse<Void>> registerMovie(@RequestBody @Valid RegisterMovieRequest request) {
        movieService.registerMovie(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.of());
    }

    @PatchMapping("/{movieId}")
    public ResponseEntity<CustomResponse<Void>> deleteMovie(@PathVariable("movieId") Long movieId) {
        movieService.deleteMovie(movieId);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<MovieListResponse>>> getMovieList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<MovieListResponse> movieList = movieService.getMovieList(keyword, pageable);

        return ResponseEntity.ok(CustomResponse.of(movieList));
    }
}
