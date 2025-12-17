package com.cgv.mega.genre.service;

import com.cgv.mega.genre.dto.GenreListResponse;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    @Transactional(readOnly = true)
    public GenreListResponse getAllGenre() {
        List<Genre> allGenres = genreRepository.findAllByOrderByIdAsc();

        return GenreListResponse.toDto(allGenres);
    }
}
