package com.cgv.mega.genre.dto;

import com.cgv.mega.genre.entity.Genre;

import java.util.List;

public record GenreListResponse(
        List<GenreInfo> genreInfoList
) {
    public record GenreInfo(
        Long id,
        String name
    ) { }

    public static GenreListResponse toDto(List<Genre> genres) {
        return new GenreListResponse(genres.stream()
                .map(g -> new GenreInfo(
                        g.getId(),
                        g.getName()
                ))
                .toList());
    }
}
