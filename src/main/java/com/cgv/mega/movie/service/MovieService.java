package com.cgv.mega.movie.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.dto.MovieInfoResponse;
import com.cgv.mega.movie.dto.RegisterMovieRequest;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieStatus;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ScreeningRepository screeningRepository;

    // 영화 등록
    @Transactional
    public void registerMovie(RegisterMovieRequest request) {
        Movie movie = Movie.createMovie(
                request.title(), request.duration(), request.description(), request.posterUrl());

        for (MovieType movieType : request.types()) {
            movie.addType(movieType);
        }

        List<Genre> genres = genreRepository.findAllById(request.genreIds());

        if (genres.size() != request.genreIds().size()) {
            throw new CustomException(ErrorCode.GENRE_NOT_FOUND);
        }

        for (Genre genre : genres) {
            movie.addGenre(genre);
        }

        movieRepository.save(movie);
    }

    // 영화 삭제
    @Transactional
    public void deleteMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));

        if (movie.getStatus().equals(MovieStatus.INACTIVE)) {
            throw new CustomException(ErrorCode.MOVIE_ALREADY_DELETED);
        }

        if (screeningRepository.existsByMovieId(movieId)) {
            throw new CustomException(ErrorCode.MOVIE_ALREADY_SCREENING);
        }

        movie.deactivate();
    }

    // 영화 상세조회
    @Transactional(readOnly = true)
    public MovieInfoResponse getMovieInfo(Long movieId) {
        Movie movie = movieRepository.findByIdWithGenresAndTypes(movieId)
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));

        Set<String> genres = movie.getMovieGenres().stream()
                .map(mg -> mg.getGenre().getName())
                .collect(Collectors.toSet());

        Set<String> types = movie.getMovieTypes().stream()
                .map(mt -> mt.getType().getValue())
                .collect(Collectors.toSet());

        return new MovieInfoResponse(movie.getTitle(), movie.getDuration(), movie.getDescription(),
                movie.getPosterUrl(), genres, types);
    }
}
