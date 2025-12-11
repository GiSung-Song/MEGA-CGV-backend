package com.cgv.mega.screening.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import com.cgv.mega.reservation.service.ReservationService;
import com.cgv.mega.screening.dto.*;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.enums.DisplayScreeningSeatStatus;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.enums.ScreeningStatus;
import com.cgv.mega.screening.repository.ScreeningQueryRepository;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.entity.SeatFixture;
import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.entity.TheaterFixture;
import com.cgv.mega.theater.enums.TheaterType;
import com.cgv.mega.theater.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

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
    private SeatRepository seatRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private ReservationGroupRepository reservationGroupRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ScreeningService screeningService;

    private Movie movie;
    private Theater theater;

    @BeforeEach
    void setUp() {
        movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
        ReflectionTestUtils.setField(movie, "id", 1L);

        theater = TheaterFixture.createTheater(1L, "1관", 50, TheaterType.SCREEN_X);
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
            List<MovieScreeningInfoDto> list =
                    List.of(new MovieScreeningInfoDto(0L, 1L, "1관", Long.valueOf(50),
                            LocalDateTime.of(2026, 11, 11, 15, 0),
                            LocalDateTime.of(2026, 11, 11, 18, 0),
                            1, ScreeningStatus.SCHEDULED));

            given(screeningQueryRepository.getMovieScreeningListForUser(1L, LocalDate.of(2026, 11, 11)))
                    .willReturn(list);

            MovieScreeningResponse response = screeningService.getMovieScreeningsForUser(1L, LocalDate.of(2026, 11, 11));

            assertThat(response.movieInfoList())
                    .hasSize(1);
        }
    }

    @Nested
    class 특정_영화_상영_목록_조회_관리자용 {
        @Test
        void 조회_성공() {
            List<MovieScreeningForAdminResponse.MovieScreeningInfo> list =
                    List.of(new MovieScreeningForAdminResponse.MovieScreeningInfo(0L, 1L, "1관", Long.valueOf(50),
                            LocalDateTime.of(2026, 11, 11, 15, 0),
                            LocalDateTime.of(2026, 11, 11, 18, 0),
                            1, ScreeningStatus.SCHEDULED));

            given(screeningQueryRepository.getMovieScreeningListForAdmin(1L)).willReturn(list);

            MovieScreeningForAdminResponse response = screeningService.getMovieScreeningsForAdmin(1L);

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
                    new ScreeningSeatDto(1L, "A", 2, SeatType.NORMAL, ScreeningSeatStatus.RESERVED, 10000),
                    new ScreeningSeatDto(2L, "A", 3, SeatType.NORMAL, ScreeningSeatStatus.FIXING, 10000),
                    new ScreeningSeatDto(3L, "A", 4, SeatType.NORMAL, ScreeningSeatStatus.RESERVED, 10000),
                    new ScreeningSeatDto(4L, "A", 5, SeatType.NORMAL, ScreeningSeatStatus.AVAILABLE, 10000),
                    new ScreeningSeatDto(5L, "A", 6, SeatType.NORMAL, ScreeningSeatStatus.RESERVED, 10000)
            );

            List<Object> redisValues = Arrays.asList(
                    "user",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            given(screeningQueryRepository.getScreeningSeat(1L)).willReturn(screeningSeat);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            given(valueOperations.multiGet(anyList())).willReturn(redisValues);

            ScreeningSeatResponse response = screeningService.getScreeningSeatStatus(1L);

            List<ScreeningSeatResponse.ScreeningSeatInfo> screeningSeatInfos = response.screeningSeatInfos();

            assertThat(response.screeningId()).isEqualTo(1L);
            assertThat(screeningSeatInfos).hasSize(6);

            assertThat(screeningSeatInfos.get(0).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(0).colNumber()).isEqualTo(1);
            assertThat(screeningSeatInfos.get(0).status()).isEqualTo(DisplayScreeningSeatStatus.HOLD);

            assertThat(screeningSeatInfos.get(1).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(1).colNumber()).isEqualTo(2);
            assertThat(screeningSeatInfos.get(1).status()).isEqualTo(DisplayScreeningSeatStatus.RESERVED);

            assertThat(screeningSeatInfos.get(2).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(2).colNumber()).isEqualTo(3);
            assertThat(screeningSeatInfos.get(2).status()).isEqualTo(DisplayScreeningSeatStatus.FIXING);

            assertThat(screeningSeatInfos.get(3).rowLabel()).isEqualTo("A");
            assertThat(screeningSeatInfos.get(3).colNumber()).isEqualTo(4);
            assertThat(screeningSeatInfos.get(3).status()).isEqualTo(DisplayScreeningSeatStatus.RESERVED);
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
    class 상영_취소 {
        @Test
        void 상영_취소() {
            Screening screening = Screening.createScreening(movie, theater,
                    LocalDateTime.of(2026, 11, 11, 10, 0),
                    LocalDateTime.of(2026, 11, 11, 13, 0),
                    1, MovieType.TWO_D
            );

            ReflectionTestUtils.setField(screening, "id", 50L);

            ReservationGroup rg1 = ReservationGroup.createReservationGroup(1L);
            ReservationGroup rg2 = ReservationGroup.createReservationGroup(2L);

            given(screeningRepository.findById(50L)).willReturn(Optional.of(screening));
            given(reservationGroupRepository.findAllByScreeningId(50L)).willReturn(List.of(rg1, rg2));

            willDoNothing().given(reservationService).cancelReservationByScreeningCancel(any(ReservationGroup.class));

            screeningService.cancelScreening(screening.getId());

            assertThat(screening.getStatus()).isEqualTo(ScreeningStatus.CANCELED);
        }

        @Test
        void 상영_없음_404반환() {
            given(screeningRepository.findById(50L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> screeningService.cancelScreening(50L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCREENING_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 상영_취소_불가_상태_409반환() {
            Screening screening = Screening.createScreening(movie, theater,
                    LocalDateTime.of(2026, 11, 11, 10, 0),
                    LocalDateTime.of(2026, 11, 11, 13, 0),
                    1, MovieType.TWO_D
            );

            screening.markEnded();

            ReflectionTestUtils.setField(screening, "id", 50L);

            given(screeningRepository.findById(50L)).willReturn(Optional.of(screening));

            assertThatThrownBy(() -> screeningService.cancelScreening(screening.getId()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCREENING_CANCEL_NOT_ALLOWED);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }
    }
}