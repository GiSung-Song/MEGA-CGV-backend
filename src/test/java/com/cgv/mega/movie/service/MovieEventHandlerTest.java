package com.cgv.mega.movie.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.dto.MovieCreatedEvent;
import com.cgv.mega.movie.dto.MovieDeletedEvent;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.entity.MovieDocument;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.movie.repository.MovieSearchRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MovieEventHandlerTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieSearchRepository movieSearchRepository;

    @InjectMocks
    private MovieEventHandler movieEventHandler;

    @Nested
    class 영화_ES_저장_테스트 {
        @Test
        void 저장_성공() {
            Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");

            given(movieRepository.findByIdWithGenresAndTypes(1L)).willReturn(Optional.of(movie));

            movieEventHandler.addMovie(new MovieCreatedEvent(1L));

            then(movieRepository).should().findByIdWithGenresAndTypes(1L);
            then(movieSearchRepository).should().save(any(MovieDocument.class));
        }

        @Test
        void 영화_없음_404반환() {
            given(movieRepository.findByIdWithGenresAndTypes(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> movieEventHandler.addMovie(new MovieCreatedEvent(1L)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    class 영화_삭제_테스트 {

        @Test
        void 영화_삭제_성공() {
            Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

            movieEventHandler.deleteMovie(new MovieDeletedEvent(1L));

            then(movieRepository).should().findById(1L);
            then(movieSearchRepository).should().deleteById(anyLong());
        }

        @Test
        void 영화_없음_실패_404반환() {
            given(movieRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> movieEventHandler.deleteMovie(new MovieDeletedEvent(1L)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}