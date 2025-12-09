package com.cgv.mega.screening.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.screening.dto.ScreeningSeatHoldDto;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.entity.SeatFixture;
import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.entity.TheaterFixture;
import com.cgv.mega.theater.enums.TheaterType;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScreeningSeatServiceTest {

    @InjectMocks
    private ScreeningSeatService screeningSeatService;

    @Mock
    private ScreeningSeatRepository screeningSeatRepository;

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private Screening screening;
    private Movie movie;
    private Theater theater;
    private List<ScreeningSeat> screeningSeats;

    @BeforeEach
    void setUp() {
        movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
        ReflectionTestUtils.setField(movie, "id", 1L);

        theater = TheaterFixture.createTheater(1L, "1관", 50, TheaterType.SCREEN_X);

        screening = Screening.createScreening(
                movie, theater,
                LocalDateTime.of(2026, 11, 11, 11, 0),
                LocalDateTime.of(2026, 11, 11, 14, 0),
                1, MovieType.TWO_D
        );

        Seat seat1 = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);
        Seat seat2 = SeatFixture.createSeat(theater, "A", 2, SeatType.NORMAL);
        Seat seat3 = SeatFixture.createSeat(theater, "A", 3, SeatType.NORMAL);

        screeningSeats = List.of(
                ScreeningSeat.createScreeningSeat(screening, seat1, 1000),
                ScreeningSeat.createScreeningSeat(screening, seat2, 1000),
                ScreeningSeat.createScreeningSeat(screening, seat3, 1000)
        );
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
                    1, MovieType.TWO_D
            );

            Seat seat = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);

            screeningSeat = ScreeningSeat.createScreeningSeat(
                    screening, seat, 1000
            );
        }

        @Test
        void 수리중_표시_성공() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));

            screeningSeatService.fixingScreeningSeat(1L);

            assertThat(screeningSeat.getStatus()).isEqualTo(ScreeningSeatStatus.FIXING);
        }

        @Test
        void 수리중_표시_실패_409반환() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));
            screeningSeat.reserveScreeningSeat();

            assertThatThrownBy(() -> screeningSeatService.fixingScreeningSeat(1L))
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
                    1, MovieType.TWO_D
            );

            Seat seat = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);

            screeningSeat = ScreeningSeat.createScreeningSeat(
                    screening, seat, 1000
            );
        }

        @Test
        void 정상_표시_성공() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));
            screeningSeat.fixScreeningSeat();

            screeningSeatService.restoringScreeningSeat(1L);

            assertThat(screeningSeat.getStatus()).isEqualTo(ScreeningSeatStatus.AVAILABLE);
        }

        @Test
        void 정상_표시_실패_409반환() {
            given(screeningSeatRepository.findById(anyLong())).willReturn(Optional.of(screeningSeat));
            screeningSeat.reserveScreeningSeat();

            assertThatThrownBy(() -> screeningSeatService.restoringScreeningSeat(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException exception = (CustomException) ex;

                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SCREENING_SEAT_CANNOT_UPDATE);
                    });
        }
    }

    @Nested
    class 좌석_홀드 {
        private Screening screening;

        @BeforeEach
        void setUp() {
            screening = Screening.createScreening(
                    movie, theater,
                    LocalDateTime.of(2026, 11, 11, 11, 0),
                    LocalDateTime.of(2026, 11, 11, 14, 0),
                    1, MovieType.TWO_D
            );
        }

        @Test
        void 홀드_성공() {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(2L, 1L, 3L));

            given(screeningRepository.findById(anyLong())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningId(any(), anyLong())).willReturn(screeningSeats);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn(null);
            given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);

            screeningSeatService.holdScreeningSeat(1L, 10L, req);

            verify(valueOperations, times(3))
                    .setIfAbsent(anyString(), eq("1"), any());

        }

        @Test
        void 상영_없음_404반환() {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(2L, 1L, 3L));

            given(screeningRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> screeningSeatService.holdScreeningSeat(1L, 10L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCREENING_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 좌석_없음_404반환() {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(2L, 1L, 3L));

            given(screeningRepository.findById(anyLong())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningId(any(), anyLong()))
                    .willReturn(List.of(screeningSeats.get(0), screeningSeats.get(1)));

            assertThatThrownBy(() -> screeningSeatService.holdScreeningSeat(1L, 10L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCREENING_SEAT_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 예약_불가능한_좌석_409반환() {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(2L, 1L, 3L));

            screeningSeats.get(0).reserveScreeningSeat();

            given(screeningRepository.findById(anyLong())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningId(any(), anyLong())).willReturn(screeningSeats);

            assertThatThrownBy(() -> screeningSeatService.holdScreeningSeat(1L, 10L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCREENING_SEAT_NOT_AVAILABLE);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 이미_홀드된_좌석_409반환() {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(2L, 1L, 3L));

            given(screeningRepository.findById(anyLong())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningId(any(), anyLong())).willReturn(screeningSeats);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            given(valueOperations.get("seat:1")).willReturn(null);
            given(valueOperations.setIfAbsent(eq("seat:1"), eq("1"), any()))
                    .willReturn(true);

            given(valueOperations.get("seat:2")).willReturn("2");
            given(valueOperations.setIfAbsent(eq("seat:2"), eq("1"), any()))
                    .willReturn(false);

            assertThatThrownBy(() -> screeningSeatService.holdScreeningSeat(1L, 10L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCREENING_SEAT_ALREADY_HOLD);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });

            verify(redisTemplate).delete("seat:1");
        }
    }

    @Nested
    class 좌석_홀드_취소 {
        @Test
        void 홀드_취소_성공() {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(3L, 1L, 2L));

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("seat:1")).willReturn("10");
            given(valueOperations.get("seat:2")).willReturn("99");
            given(valueOperations.get("seat:3")).willReturn("10");

            screeningSeatService.cancelHoldScreeningSeat(10L, req);

            verify(redisTemplate).delete("seat:1");
            verify(redisTemplate).delete("seat:3");
            verify(redisTemplate, never()).delete("seat:2");
        }
    }
}