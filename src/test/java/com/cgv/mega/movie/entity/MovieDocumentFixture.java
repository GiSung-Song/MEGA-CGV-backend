package com.cgv.mega.movie.entity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public final class MovieDocumentFixture {

    private MovieDocumentFixture() {}

    public static MovieDocument create(Long id, String title, Set<String> genres, Set<String> types, String posterUrl) {
        return new MovieDocument(
                id, title, genres, types, posterUrl
        );
    }
}