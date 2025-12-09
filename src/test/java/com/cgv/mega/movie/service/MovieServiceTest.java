package com.cgv.mega.movie.service;

import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.entity.GenreFixture;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.dto.*;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.entity.MovieDocument;
import com.cgv.mega.movie.entity.MovieDocumentFixture;
import com.cgv.mega.movie.enums.MovieStatus;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.movie.repository.MovieSearchRepository;
import com.cgv.mega.screening.repository.ScreeningRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private MovieSearchRepository movieSearchRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MovieService movieService;

    @Nested
    class 영화_등록_테스트 {

        @Test
        void 영화_등록_성공() {
            Set<MovieType> types = Set.of(MovieType.TWO_D, MovieType.THREE_D);
            Set<Long> genreIds = Set.of(1L, 2L);

            Genre action = GenreFixture.create(1L, "ACTION");
            Genre drama = GenreFixture.create(2L, "DRAMA");

            RegisterMovieRequest request = new RegisterMovieRequest(
                    "인터스텔라", 150, "인터스텔라 설명", "poster.png", types, genreIds);

            given(genreRepository.findAllById(request.genreIds())).willReturn(List.of(action, drama));

            movieService.registerMovie(request);

            then(genreRepository).should().findAllById(request.genreIds());
            then(movieRepository).should().save(any(Movie.class));
            then(eventPublisher).should().publishEvent(any(MovieCreatedEvent.class));
        }

        @Test
        void 존재하지_않는_장르_추가_실패_404반환() {
            Set<MovieType> types = Set.of(MovieType.TWO_D, MovieType.THREE_D);
            Set<Long> genreIds = Set.of(1L, 2L);

            Genre action = GenreFixture.create(1L, "ACTION");

            RegisterMovieRequest request = new RegisterMovieRequest(
                    "인터스텔라", 150, "인터스텔라 설명", "poster.png", types, genreIds);

            given(genreRepository.findAllById(request.genreIds())).willReturn(List.of(action));

            assertThatThrownBy(() -> movieService.registerMovie(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.GENRE_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            then(movieRepository).should(never()).save(any(Movie.class));
        }
    }

    @Nested
    class 영화_삭제_테스트 {
        @Test
        void 영화_삭제_정상() {
            Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
            ReflectionTestUtils.setField(movie, "id", 1L);

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(screeningRepository.existsByMovieId(1L)).willReturn(false);

            movieService.deleteMovie(1L);

            assertThat(movie.getStatus()).isEqualTo(MovieStatus.INACTIVE);
            then(eventPublisher).should().publishEvent(any(MovieDeletedEvent.class));
        }

        @Test
        void 존재하지_않는_영화_404반환() {
            given(movieRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.deleteMovie(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 이미_삭제된_영화_409반환() {
            Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
            ReflectionTestUtils.setField(movie, "id", 1L);
            ReflectionTestUtils.setField(movie, "status", MovieStatus.INACTIVE);

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));

            assertThatThrownBy(() -> movieService.deleteMovie(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_ALREADY_DELETED);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 상영되었거나_상영된_영화_삭제_불가능_400반환() {
            Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
            ReflectionTestUtils.setField(movie, "id", 1L);

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(screeningRepository.existsByMovieId(1L)).willReturn(true);

            assertThatThrownBy(() -> movieService.deleteMovie(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_ALREADY_SCREENING);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }
    }

    @Nested
    class 영화_상세조회_테스트 {

        @Test
        void 영화_상세조회_성공() {
            Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
            ReflectionTestUtils.setField(movie, "id", 1L);
            Genre action = GenreFixture.create(1L, "ACTION");
            Genre drama = GenreFixture.create(2L, "DRAMA");

            movie.addGenre(action);
            movie.addGenre(drama);
            movie.addType(MovieType.TWO_D);

            given(movieRepository.findByIdWithGenresAndTypes(1L)).willReturn(Optional.of(movie));

            MovieInfoResponse movieInfo = movieService.getMovieInfo(1L);

            assertThat(movieInfo.genres().size()).isEqualTo(2);
            assertThat(movieInfo.title()).isEqualTo("혹성탈출");
            assertThat(movieInfo.types()).containsExactly("2D");
            assertThat(movieInfo.genres()).containsExactlyInAnyOrder("ACTION", "DRAMA");
        }

        @Test
        void 존재하지_않는_영화_404반환() {
            given(movieRepository.findByIdWithGenresAndTypes(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.getMovieInfo(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    class 영화_목록_조회_테스트 {

        @Test
        void 조회_결과_0건() {
            Pageable pageable = PageRequest.of(0, 10);

            PageResponse<MovieListResponse> movieList = movieService.getMovieList("", pageable);

            assertThat(movieList.content()).isEmpty();
            assertThat(movieList.pageInfo().totalElements()).isEqualTo(0);
        }

        @Test
        void 조회_결과_반환() {
            Pageable pageable = PageRequest.of(0, 10);
            MovieDocument doc = MovieDocumentFixture.create(1L, "혹성탈출", Set.of("ACTION", "DRAMA"), Set.of("TWO_D", "THREE_D"), "poster@png.com");

            Page<MovieDocument> page = new PageImpl<>(List.of(doc), pageable, 1);
            given(movieSearchRepository.searchByTitle("혹성", pageable)).willReturn(page);

            PageResponse<MovieListResponse> result = movieService.getMovieList("혹성", pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).title()).isEqualTo("혹성탈출");
            assertThat(result.pageInfo().totalElements()).isEqualTo(1);
        }
    }
}