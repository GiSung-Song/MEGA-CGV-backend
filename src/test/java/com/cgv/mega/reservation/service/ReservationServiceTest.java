package com.cgv.mega.reservation.service;

import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.payment.enums.PaymentStatus;
import com.cgv.mega.payment.service.PaymentService;
import com.cgv.mega.reservation.dto.*;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.enums.ReservationStatus;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import com.cgv.mega.reservation.repository.ReservationQueryRepository;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.screening.service.ScreeningSeatService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationQueryRepository reservationQueryRepository;

    @Mock
    private ReservationGroupRepository reservationGroupRepository;

    @Mock
    private ScreeningSeatRepository screeningSeatRepository;

    @Mock
    private ScreeningSeatService screeningSeatService;

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PaymentService paymentService;


    @InjectMocks
    private ReservationService reservationService;

    private Movie movie;
    private Theater theater;
    private Screening screening;
    private Seat a1;
    private Seat a2;
    private ScreeningSeat seatA1;
    private ScreeningSeat seatA2;

    @BeforeEach
    void setUp() {
        theater = TheaterFixture.createTheater(1L, "1관", 10, TheaterType.FOUR_DX);
        a1 = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);
        a2 = SeatFixture.createSeat(theater, "A", 2, SeatType.NORMAL);

        movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
        ReflectionTestUtils.setField(movie, "id", 1L);

        screening = Screening.createScreening(movie, theater,
                LocalDateTime.of(2026, 11, 11, 10, 0),
                LocalDateTime.of(2026, 11, 11, 13, 0),
                1, MovieType.TWO_D);

        ReflectionTestUtils.setField(screening, "id", 99L);

        seatA1 = ScreeningSeat.createScreeningSeat(screening, a1, 1000);
        seatA2 = ScreeningSeat.createScreeningSeat(screening, a2, 1000);

        ReflectionTestUtils.setField(seatA1, "id", 1L);
        ReflectionTestUtils.setField(seatA2, "id", 2L);
    }

    @Nested
    class 예약_생성 {
        @Test
        void 예약_생성_성공() {
            Long userId = 1L;
            Long screeningId = screening.getId();
            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L));

            given(screeningRepository.findById(screening.getId())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningIdForUpdate(Set.of(seatA1.getId(), seatA2.getId()), screening.getId()))
                    .willReturn(List.of(seatA1, seatA2));

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.multiGet(anyList())).willReturn(List.of(String.valueOf(userId), String.valueOf(userId)));

            given(reservationGroupRepository.save(any(ReservationGroup.class)))
                    .willAnswer(invocation -> {
                        ReservationGroup rg = invocation.getArgument(0);
                        ReflectionTestUtils.setField(rg, "id", 10L);

                        return rg;
                    });

            ReservationGroup result = reservationService.createReservation(userId, screeningId, request);

            verify(screeningRepository).findById(screeningId);
            verify(screeningSeatRepository).findByIdInAndScreeningIdForUpdate(
                    Set.of(seatA1.getId(), seatA2.getId()), screeningId);
            verify(redisTemplate).opsForValue();
            verify(valueOperations).multiGet(anyList());
            verify(screeningSeatService).reserveScreeningSeat(List.of(seatA1, seatA2));
            verify(reservationGroupRepository).save(any(ReservationGroup.class));
            verify(eventPublisher).publishEvent(any(DeleteScreeningSeatKeyEvent.class));

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getReservations()).hasSize(2);
            assertThat(result.getTotalPrice()).isEqualTo(2000);
        }

        @Test
        void 상영회차_없음_404반환() {
            Long userId = 1L;
            Long screeningId = screening.getId();
            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L));

            given(screeningRepository.findById(screeningId)).willThrow(new CustomException(ErrorCode.SCREENING_NOT_FOUND));

            assertThatThrownBy(() -> reservationService.createReservation(userId, screeningId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 예약가능_여부_실패_400반환() {
            Long userId = 1L;
            Long screeningId = screening.getId();
            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L));

            given(screeningRepository.findById(screening.getId())).willReturn(Optional.of(screening));
            screening.cancelScreening();

            assertThatThrownBy(() -> reservationService.createReservation(userId, screeningId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        void 좌석_없음_404반환() {
            Long userId = 1L;
            Long screeningId = screening.getId();
            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L));

            given(screeningRepository.findById(screening.getId())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningIdForUpdate(Set.of(seatA1.getId(), seatA2.getId()), screening.getId()))
                    .willReturn(List.of(seatA1));

            assertThatThrownBy(() -> reservationService.createReservation(userId, screeningId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 자신의_좌석이_아닌경우_409반환() {
            Long userId = 1L;
            Long screeningId = screening.getId();
            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L));

            given(screeningRepository.findById(screening.getId())).willReturn(Optional.of(screening));
            given(screeningSeatRepository.findByIdInAndScreeningIdForUpdate(Set.of(seatA1.getId(), seatA2.getId()), screening.getId()))
                    .willReturn(List.of(seatA1, seatA2));

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.multiGet(anyList())).willReturn(List.of(String.valueOf(1651446L), String.valueOf(1234231L)));

            assertThatThrownBy(() -> reservationService.createReservation(userId, screeningId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }
    }

    @Nested
    class 예약_목록_조회 {
        @Test
        void 조회_성공() {
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            List<ReservationListDto> dtoList = new ArrayList<>();

            ReservationListDto dto = new ReservationListDto(
                1L, movie.getTitle(), screening.getMovieType(), screening.getStartTime(),
                    theater.getName(), theater.getType(),
                    List.of(new ReservationListDto.SeatDto(a1.getRowLabel(), a1.getColNumber(), a1.getType()),
                            new ReservationListDto.SeatDto(a2.getRowLabel(), a2.getColNumber(), a2.getType())),
                    ReservationStatus.PAID, 15000, movie.getPosterUrl(),
                    LocalDateTime.of(2026, 11, 11, 15, 0)
            );

            dtoList.add(dto);

            Page<ReservationListDto> page = new PageImpl<>(dtoList, pageable, 1);

            given(reservationQueryRepository.getReservationList(userId, pageable)).willReturn(page);

            PageResponse<ReservationListResponse> response = reservationService.getReservationList(userId, pageable);

            assertThat(response.pageInfo().totalElements()).isEqualTo(1);
            assertThat(response.pageInfo().page()).isEqualTo(0);
            assertThat(response.content().get(0).title()).isEqualTo(movie.getTitle());
            assertThat(response.content().get(0).seats().size()).isEqualTo(2);
        }
    }

    @Nested
    class 상영_취소로_예약_전체_취소 {
        @Test
        void 취소_성공() {
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(1L);

            reservationService.cancelReservationByScreeningCancel(reservationGroup);

            assertThat(reservationGroup.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Nested
    class 예약_취소 {
        @Test
        void 취소_성공() {
            Long userId = 1L;
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(userId);
            ReflectionTestUtils.setField(reservationGroup, "id", 99L);

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId)).willReturn(Optional.of(reservationGroup));
            given(screeningRepository.findScreeningStartTime(reservationGroup.getId(), userId)).willReturn(screening.getStartTime());

            reservationService.cancelReservation(userId, reservationGroup.getId());

            assertThat(reservationGroup.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void 예약_없음_404반환() {
            Long userId = 1L;

            given(reservationGroupRepository.findByIdAndUserId(anyLong(), eq(userId))).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancelReservation(userId, 99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 취소_불가능_409반환() {
            Long userId = 1L;
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(userId);
            ReflectionTestUtils.setField(reservationGroup, "id", 99L);

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId)).willReturn(Optional.of(reservationGroup));
            given(screeningRepository.findScreeningStartTime(reservationGroup.getId(), userId)).willReturn(
                    LocalDateTime.now().plusMinutes(5)
            );

            assertThatThrownBy(() -> reservationService.cancelReservation(userId, 99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }
    }

    @Nested
    class 예약_상세_조회 {
        @Test
        void 조회_성공() {
            Long userId = 1L;
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(userId);
            ReflectionTestUtils.setField(reservationGroup, "id", 99L);

            reservationGroup.successReservation();

            ReservationDetailDto reservationDetailDto = new ReservationDetailDto(
                    movie.getId(), movie.getTitle(), screening.getMovieType(), movie.getPosterUrl(), movie.getDuration(), screening.getId(),
                    screening.getStartTime(), screening.getEndTime(), theater.getId(), theater.getName(), theater.getType(),
                    List.of(new ReservationDetailDto.SeatInfo(a1.getRowLabel(), a1.getColNumber(), a1.getType()),
                            new ReservationDetailDto.SeatInfo(a2.getRowLabel(), a2.getColNumber(), a2.getType())),
                    reservationGroup.getId(), reservationGroup.getStatus(), reservationGroup.getCreatedAt(),
                    reservationGroup.getUpdatedAt(), PaymentStatus.COMPLETED, "card", BigDecimal.valueOf(15000.00),
                    BigDecimal.ZERO, "merchant-uid-1", "payment-id-1", "buyer", "01012341234", "a@b.com"
            );

            given(reservationQueryRepository.getReservationDetail(userId, reservationGroup.getId())).willReturn(reservationDetailDto);

            ReservationDetailResponse response = reservationService.getReservationDetail(userId, reservationGroup.getId());

            assertThat(response.title()).isEqualTo(movie.getTitle());
            assertThat(response.seatInfos().size()).isEqualTo(2);
            assertThat(response.startTime()).isEqualTo(screening.getStartTime());
            assertThat(response.reservationCancelledAt()).isNull();
            assertThat(response.paymentAmount()).isEqualTo(BigDecimal.valueOf(15000.00));
        }

        @Test
        void 예약_없음_404반환() {
            Long userId = 1L;

            given(reservationQueryRepository.getReservationDetail(eq(userId), anyLong())).willReturn(null);

            assertThatThrownBy(() -> reservationService.getReservationDetail(userId, 99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}