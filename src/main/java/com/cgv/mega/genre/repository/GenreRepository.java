package com.cgv.mega.genre.repository;

import com.cgv.mega.genre.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    List<Genre> findAllByOrderByIdAsc();
}
