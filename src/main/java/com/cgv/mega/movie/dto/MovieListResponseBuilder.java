package com.cgv.mega.movie.dto;

import java.util.Set;

public record MovieListResponseBuilder(
        Long id,
        String title,
        Set<String> genres,
        Set<String> types,
        String posterUrl
) {
    public MovieListResponse build() {
        return new MovieListResponse(
                id, title, genres, types, posterUrl
        );
    }
}
