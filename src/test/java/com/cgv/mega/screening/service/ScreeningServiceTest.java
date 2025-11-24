package com.cgv.mega.screening.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.common.enums.SeatType;
import com.cgv.mega.common.enums.TheaterType;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.screening.dto.*;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.repository.ScreeningQueryRepository;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.entity.SeatFixture;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.entity.TheaterFixture;
import com.cgv.mega.theater.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private ScreeningQueryRepository screeningQueryRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TheaterRepository theaterRepository;

    @Mock
    private ScreeningSeatRepository screeningSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private ScreeningService screeningService;

    private Movie movie;
    private Theater theater;

    @BeforeEach
    void setUp() {
        movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
        ReflectionTestUtils.setField(movie, "id", 1L);

        theater = TheaterFixture.createTheater(1L, "1관", 50, TheaterType.SCREEN_X, 20000);
    }

    @Nested
    class 상영_등록_가능_시간_조회 {

        @Test
        void 조회_성공() {
            LocalDate date = LocalDate.of(2026, 11, 11);

            List<ScreeningTimeDto> screeningTimeDtoList = List.of(
                    new ScreeningTimeDto(LocalDateTime.of(2026, 11, 11, 8, 0),
                            LocalDateTime.of(2026, 11, 11, 11, 0)),
                    new ScreeningTimeDto(LocalDateTime.of(2026, 11, 11, 22, 0),
                            LocalDateTime.of(2026, 11, 12, 1, 0))
            );

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.of(theater));
            given(screeningQueryRepository.getReservedScreening(1L, date)).willReturn(screeningTimeDtoList);

            AvailableScreeningResponse response = screeningService.getAvailableScreeningTime(1L, 1L, date);

            assertThat(response).isNotNull();
            assertThat(response.availableTime()).isNotEmpty();
        }

        @Test
        void 이전_날짜는_400반환() {
            LocalDate date = LocalDate.of(2014, 3, 10);

            assertThatThrownBy(() -> screeningService.getAvailableScreeningTime(1L, 1L, date))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SCREENING_START_TIME);
                    });
        }

        @Test
        void 영화_없음_404반환() {
            LocalDate date = LocalDate.of(2026, 11, 11);

            given(movieRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> screeningService.getAvailableScreeningTime(1L, 1L, date))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
                    });
        }

        @Test
        void 상영관_없음_404반환() {
            LocalDate date = LocalDate.of(2026, 11, 11);

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> screeningService.getAvailableScreeningTime(1L, 1L, date))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.THEATER_NOT_FOUND);
                    });
        }
    }

    @Nested
    class 상영회차_추가 {
        @Test
        void 추가_정상() {
            LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 10, 0);
            LocalDateTime endTime = startTime.plusMinutes(10).plusMinutes(movie.getDuration());

            RegisterScreeningRequest req = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, startTime
            );

            Set<Seat> seats = SeatFixture.defaultSeats(theater);

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.of(theater));
            given(screeningQueryRepository.existsOverlap(1L, startTime, endTime)).willReturn(false);
            given(screeningQueryRepository.getMovieSequence(1L)).willReturn(1);
            given(seatRepository.findByTheaterId(1L)).willReturn(seats);

            screeningService.registerScreening(req);

            then(screeningRepository).should().save(any(Screening.class));
        }

        @Test
        void 영화_없음_404반환() {
            LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 10, 0);

            RegisterScreeningRequest req = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, startTime
            );

            given(movieRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> screeningService.registerScreening(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
                    });
        }

        @Test
        void 상영관_없음_404반환() {
            LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 10, 0);

            RegisterScreeningRequest req = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, startTime
            );

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> screeningService.registerScreening(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.THEATER_NOT_FOUND);
                    });
        }

        @Test
        void 상영_불가능한_시간_400반환() {
            LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 3, 0);

            RegisterScreeningRequest req = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, startTime
            );

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.of(theater));

            assertThatThrownBy(() -> screeningService.registerScreening(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SCREENING_START_TIME);
                    });
        }

        @Test
        void 겹치는_시간_존재_409반환() {
            LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 8, 0);
            LocalDateTime endTime = startTime.plusMinutes(10).plusMinutes(movie.getDuration());

            RegisterScreeningRequest req = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, startTime
            );

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.of(theater));
            given(screeningQueryRepository.existsOverlap(1L, startTime, endTime)).willReturn(true);

            assertThatThrownBy(() -> screeningService.registerScreening(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_THEATER_SCREENING_TIME);
                    });
        }

        @Test
        void 상영관_좌석_없을_시_404반환() {
            LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 10, 0);
            LocalDateTime endTime = startTime.plusMinutes(10).plusMinutes(movie.getDuration());

            RegisterScreeningRequest req = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, startTime
            );

            given(movieRepository.findById(1L)).willReturn(Optional.of(movie));
            given(theaterRepository.findById(1L)).willReturn(Optional.of(theater));
            given(screeningQueryRepository.existsOverlap(1L, startTime, endTime)).willReturn(false);
            given(screeningQueryRepository.getMovieSequence(1L)).willReturn(1);
            given(seatRepository.findByTheaterId(1L)).willReturn(Collections.emptySet());

            assertThatThrownBy(() -> screeningService.registerScreening(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SEAT_NOT_FOUND);
                    });
        }
    }

    @Nested
    class 특정_날짜_상영_영화_조회 {
        @Test
        void 조회_성공() {
            LocalDate date = LocalDate.of(2026, 11, 11);
            List<ScreeningDateMovieResponse.MovieInfo> list =
                    List.of(new ScreeningDateMovieResponse.MovieInfo(0L, "괴물", "monster@png.com"),
                            new ScreeningDateMovieResponse.MovieInfo(1L, "타짜", "card@png.com"));

            given(screeningQueryRepository.getScreeningMovieList(date)).willReturn(list);

            ScreeningDateMovieResponse response = screeningService.getScreeningMovies(date);

            assertThat(response.movieInfos())
                    .hasSize(2)
                    .extracting(m -> m.title())
                    .containsExactlyInAnyOrder("괴물", "타짜");
        }
    }

    @Nested
    class 특정_영화_상영_목록_조회 {
        @Test
        void 조회_성공() {
            List<MovieScreeningResponse.MovieScreeningInfo> list =
                    List.of(new MovieScreeningResponse.MovieScreeningInfo(0L, 1L, "1관", Long.valueOf(50),
                            LocalDateTime.of(2026, 11, 11, 15, 0),
                            LocalDateTime.of(2026, 11, 11, 18, 0),
                            1));

            given(screeningQueryRepository.getMovieScreeningList(1L, LocalDate.of(2026, 11, 11))).willReturn(list);

            MovieScreeningResponse response = screeningService.getMovieScreenings(1L, LocalDate.of(2026, 11, 11));

            assertThat(response.movieScreeningInfos())
                    .hasSize(1);
        }
    }

    @Nested
    class 특정_영화_상영_목록_조회_관리자용 {
        @Test
        void 조회_성공() {
            List<MovieScreeningResponse.MovieScreeningInfo> list =
                    List.of(new MovieScreeningResponse.MovieScreeningInfo(0L, 1L, "1관", Long.valueOf(50),
                            LocalDateTime.of(2026, 11, 11, 15, 0),
                            LocalDateTime.of(2026, 11, 11, 18, 0),
                            1));

            given(screeningQueryRepository.getMovieScreeningList(1L, null)).willReturn(list);

            MovieScreeningResponse response = screeningService.getMovieScreeningsForAdmin(1L);

            assertThat(response.movieScreeningInfos())
                    .hasSize(1);
        }
    }

    @Nested
    class 해당_상영회차_좌석_현황_조회 {
        @Test
        void 조회_성공() {
            List<ScreeningSeatDto> screeningSeat = List.of(
                    new ScreeningSeatDto(0L, "A", 1, SeatType.NORMAL, ScreeningSeatStatus.AVAILABLE, 10000),
                    new ScreeningSeatDto(1L, "A", 2, SeatType.NORMAL, ScreeningSeatStatus.BLOCKED, 10000),
                    new ScreeningSeatDto(2L, "A", 3, SeatType.NORMAL, ScreeningSeatStatus.FIXING, 10000),
                    new ScreeningSeatDto(3L, "A", 4, SeatType.NORMAL, ScreeningSeatStatus.RESERVED, 10000),
                    new ScreeningSeatDto(4L, "A", 5, SeatType.NORMAL, ScreeningSeatStatus.AVAILABLE, 10000),
                    new ScreeningSeatDto(5L, "A", 6, SeatType.NORMAL, ScreeningSeatStatus.RESERVED, 10000)
            );

            given(screeningQueryRepository.getScreeningSeat(1L)).willReturn(screeningSeat);

            ScreeningSeatResponse response = screeningService.getScreeningSeatStatus(1L);

            List<ScreeningSeatResponse.ScreeningSeatInfo> screeningSeatInfos = response.screeningSeatInfos();

            assertThat(response.basePrice()).isEqualTo(screeningSeat.get(0).basePrice());
            assertThat(response.screeningId()).isEqualTo(1L);
            assertThat(screeningSeatInfos).hasSize(6);

            assertThat(screeningSeatInfos.get(0).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(0).colNumber()).isEqualTo(1);
            assertThat(screeningSeatInfos.get(0).status()).isEqualTo(ScreeningSeatStatus.AVAILABLE);

            assertThat(screeningSeatInfos.get(1).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(1).colNumber()).isEqualTo(2);
            assertThat(screeningSeatInfos.get(1).status()).isEqualTo(ScreeningSeatStatus.BLOCKED);

            assertThat(screeningSeatInfos.get(2).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(2).colNumber()).isEqualTo(3);
            assertThat(screeningSeatInfos.get(2).status()).isEqualTo(ScreeningSeatStatus.FIXING);

            assertThat(screeningSeatInfos.get(3).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(3).colNumber()).isEqualTo(4);
            assertThat(screeningSeatInfos.get(3).status()).isEqualTo(ScreeningSeatStatus.RESERVED);
        }

        @Test
        void 좌석_없음_404반환() {
            given(screeningQueryRepository.getScreeningSeat(1L)).willReturn(Collections.emptyList());

            assertThatThrownBy(() -> screeningService.getScreeningSeatStatus(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SEAT_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    class 좌석_수리_상태변경_테스트 {
        ScreeningSeat screeningSeat;

        @BeforeEach
        void setUp() {
            Screening screening = Screening.createScreening(
                    movie, theater,
                    LocalDateTime.of(2026, 11, 11, 11, 0),
                    LocalDateTime.of(2026, 11, 11, 14, 0),
                    1
            );

            Seat seat = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);

            screeningSeat = ScreeningSeat.createScreeningSeat(
                    screening, seat
            );
        }

        @Test
        void 수리중_표시_성공() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));

            screeningService.fixingScreeningSeat(1L);

            assertThat(screeningSeat.getStatus()).isEqualTo(ScreeningSeatStatus.FIXING);
        }

        @Test
        void 수리중_표시_실패_409반환() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));
            screeningSeat.blockScreeningSeat();

            assertThatThrownBy(() -> screeningService.fixingScreeningSeat(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException exception = (CustomException) ex;

                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SCREENING_SEAT_CANNOT_UPDATE);
                    });
        }
    }

    @Nested
    class 좌석_정상_상태변경_테스트 {
        ScreeningSeat screeningSeat;

        @BeforeEach
        void setUp() {
            Screening screening = Screening.createScreening(
                    movie, theater,
                    LocalDateTime.of(2026, 11, 11, 11, 0),
                    LocalDateTime.of(2026, 11, 11, 14, 0),
                    1
            );

            Seat seat = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);

            screeningSeat = ScreeningSeat.createScreeningSeat(
                    screening, seat
            );
        }

        @Test
        void 정상_표시_성공() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));
            screeningSeat.fixScreeningSeat();

            screeningService.restoringScreeningSeat(1L);

            assertThat(screeningSeat.getStatus()).isEqualTo(ScreeningSeatStatus.AVAILABLE);
        }

        @Test
        void 정상_표시_실패_409반환() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));
            screeningSeat.blockScreeningSeat();

            assertThatThrownBy(() -> screeningService.restoringScreeningSeat(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException exception = (CustomException) ex;

                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SCREENING_SEAT_CANNOT_UPDATE);
                    });
        }
    }
}