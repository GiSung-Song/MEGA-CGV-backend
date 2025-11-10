package com.cgv.mega.genre.entity;

import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public final class GenreFixture {

    private GenreFixture() {}

    public static Genre create(Long id, String name) {
        Genre genre = new Genre();

        ReflectionTestUtils.setField(genre, "id", id);
        ReflectionTestUtils.setField(genre, "name", name);

        return genre;
    }

}