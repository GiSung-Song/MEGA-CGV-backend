package com.cgv.mega.movie.dto;

import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.entity.MovieDocument;

import java.util.Set;
import java.util.stream.Collectors;

public record MovieListResponse(
    Long id,
    String title,
    Set<String> genres,
    Set<String> types,
    String posterUrl
) {
    public static MovieListResponse from(MovieDocument document) {
        return new MovieListResponse(
                document.getId(),
                document.getTitle(),
                document.getGenres(),
                document.getTypes(),
                document.getPosterUrl()
        );
    }
}
