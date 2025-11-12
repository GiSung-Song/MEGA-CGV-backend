package com.cgv.mega.movie.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.dto.MovieCreatedEvent;
import com.cgv.mega.movie.dto.MovieDeletedEvent;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.entity.MovieDocument;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.movie.repository.MovieSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MovieEventHandler {
    private final MovieRepository movieRepository;
    private final MovieSearchRepository movieSearchRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void addMovie(MovieCreatedEvent event) {
        Movie movie = movieRepository.findByIdWithGenresAndTypes(event.movieId())
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));

        movieSearchRepository.save(MovieDocument.from(movie));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteMovie(MovieDeletedEvent event) {
        movieRepository.findById(event.movieId())
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));

        movieSearchRepository.deleteById(event.movieId());
    }
}