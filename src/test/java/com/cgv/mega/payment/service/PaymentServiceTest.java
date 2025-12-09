package com.cgv.mega.payment.service;

import com.cgv.mega.booking.dto.BuyerInfoDto;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.payment.dto.PaymentCompleteRequest;
import com.cgv.mega.payment.dto.PortOneCancelRequest;
import com.cgv.mega.payment.dto.PortOnePaymentResponse;
import com.cgv.mega.payment.dto.RefundResult;
import com.cgv.mega.payment.entity.Payment;
import com.cgv.mega.payment.enums.PaymentStatus;
import com.cgv.mega.payment.repository.PaymentRepository;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.enums.ReservationStatus;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationGroupRepository reservationGroupRepository;

    @Mock
    private PortOneClient portOneClient;

    @InjectMocks
    private PaymentService paymentService;

    private ReservationGroup reservationGroup;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        reservationGroup = ReservationGroup.createReservationGroup(userId);
        ReflectionTestUtils.setField(reservationGroup, "id", 99L);

        Movie movie = Movie.createMovie("혹성탈출", 150, "혹성탈출 설명", "escape.png");
        ReflectionTestUtils.setField(movie, "id", 1L);

        Theater theater = TheaterFixture.createTheater(1L, "1관", 50, TheaterType.SCREEN_X);

        Screening screening = Screening.createScreening(
                movie, theater,
                LocalDateTime.of(2026, 11, 11, 11, 0),
                LocalDateTime.of(2026, 11, 11, 14, 0),
                1, MovieType.TWO_D
        );

        Seat seat1 = SeatFixture.createSeat(theater, "A", 1, SeatType.NORMAL);
        Seat seat2 = SeatFixture.createSeat(theater, "A", 2, SeatType.NORMAL);

        ScreeningSeat ss1 = ScreeningSeat.createScreeningSeat(screening, seat1, 1000);
        ScreeningSeat ss2 = ScreeningSeat.createScreeningSeat(screening, seat2, 1000);

        reservationGroup.addReservation(ss1);
        reservationGroup.addReservation(ss2);
    }

    @Nested
    class 결제_데이터_생성 {
        @Test
        void 생성_성공() {
            BuyerInfoDto dto = new BuyerInfoDto("user", "01012341234", "a@b.com");

            given(paymentRepository.save(any(Payment.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            Payment payment = paymentService.createPayment(reservationGroup, dto);

            verify(paymentRepository).save(any(Payment.class));

            assertThat(payment.getExpectedAmount()).isEqualTo(BigDecimal.valueOf(reservationGroup.getTotalPrice()));
            assertThat(payment.getMerchantUid()).isNotNull();
        }
    }

    @Nested
    class 결제_검증 {
        private Payment payment;
        private PortOnePaymentResponse response;
        private PaymentCompleteRequest request;

        @BeforeEach
        void setUp() {
            payment = Payment.createPayment(reservationGroup,
                    "테스터", "01012341234", "a@b.com",
                    "merchant-uid", BigDecimal.valueOf(reservationGroup.getTotalPrice())
            );

            response = new PortOnePaymentResponse(
                    new PortOnePaymentResponse.Payment(
                            "payment-id", "merchant-uid", "PAID",
                            new PortOnePaymentResponse.Amount(BigDecimal.valueOf(reservationGroup.getTotalPrice()), BigDecimal.ZERO),
                            new PortOnePaymentResponse.Method("card", "SAMSUNG", "123412341234", 0),
                            null, null, null
                    )
            );

            request = new PaymentCompleteRequest(
                    payment.getMerchantUid(), response.payment().id(), response.payment().amount().total(), reservationGroup.getId()
            );
        }

        @Test
        void 검증_성공() {
            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));
            given(paymentRepository.findByMerchantUid(payment.getMerchantUid())).willReturn(Optional.of(payment));
            given(portOneClient.getPaymentInfo(request.paymentId())).willReturn(response);

            paymentService.verifyAndCompletePayment(userId, request);

            assertThat(payment.getPaymentId()).isEqualTo(response.payment().id());
            assertThat(payment.getPaidAmount()).isEqualTo(response.payment().amount().total());
            assertThat(payment.getPgProvider()).isEqualTo(response.payment().method().provider());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(reservationGroup.getStatus()).isEqualTo(ReservationStatus.PAID);
        }

        @Test
        void 예약_없음_404반환() {
            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 결제_없음_404반환() {
            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));

            given(paymentRepository.findByMerchantUid(request.merchantUid()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 해당_예약_결제_아님_실패_404반환() {
            ReservationGroup rg = ReservationGroup.createReservationGroup(321L);
            ReflectionTestUtils.setField(rg, "id", 515151L);

            Payment fakePayment = Payment.createPayment(rg,
                    "테스터", "01012341234", "a@b.com",
                    "merchant-uid", BigDecimal.valueOf(50000)
            );

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));
            given(paymentRepository.findByMerchantUid(payment.getMerchantUid())).willReturn(Optional.of(fakePayment));

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 거래_고유ID_다름_409반환() {
            response = new PortOnePaymentResponse(
                    new PortOnePaymentResponse.Payment(
                            "payment-idddd", "merchant-uid", "PAID",
                            new PortOnePaymentResponse.Amount(BigDecimal.valueOf(reservationGroup.getTotalPrice()), BigDecimal.ZERO),
                            new PortOnePaymentResponse.Method("card", "SAMSUNG", "123412341234", 0),
                            null, null, null
                    )
            );

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));
            given(paymentRepository.findByMerchantUid(payment.getMerchantUid())).willReturn(Optional.of(payment));
            given(portOneClient.getPaymentInfo(request.paymentId())).willReturn(response);

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 서비스_거래ID_다름_409반환() {
            response = new PortOnePaymentResponse(
                    new PortOnePaymentResponse.Payment(
                            "payment-id", "merchant-uidddd", "PAID",
                            new PortOnePaymentResponse.Amount(BigDecimal.valueOf(reservationGroup.getTotalPrice()), BigDecimal.ZERO),
                            new PortOnePaymentResponse.Method("card", "SAMSUNG", "123412341234", 0),
                            null, null, null
                    )
            );

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));
            given(paymentRepository.findByMerchantUid(payment.getMerchantUid())).willReturn(Optional.of(payment));
            given(portOneClient.getPaymentInfo(request.paymentId())).willReturn(response);

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 금액_검증_다름_409반환() {
            response = new PortOnePaymentResponse(
                    new PortOnePaymentResponse.Payment(
                            "payment-id", "merchant-uid", "PAID",
                            new PortOnePaymentResponse.Amount(BigDecimal.valueOf(50000000), BigDecimal.ZERO),
                            new PortOnePaymentResponse.Method("card", "SAMSUNG", "123412341234", 0),
                            null, null, null
                    )
            );

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));
            given(paymentRepository.findByMerchantUid(payment.getMerchantUid())).willReturn(Optional.of(payment));
            given(portOneClient.getPaymentInfo(request.paymentId())).willReturn(response);

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 결제_상태_검증_409반환() {
            response = new PortOnePaymentResponse(
                    new PortOnePaymentResponse.Payment(
                            "payment-id", "merchant-uid", "CANCELLED",
                            new PortOnePaymentResponse.Amount(BigDecimal.valueOf(reservationGroup.getTotalPrice()), BigDecimal.ZERO),
                            new PortOnePaymentResponse.Method("card", "SAMSUNG", "123412341234", 0),
                            null, null, null
                    )
            );

            given(reservationGroupRepository.findByIdAndUserId(reservationGroup.getId(), userId))
                    .willReturn(Optional.of(reservationGroup));
            given(paymentRepository.findByMerchantUid(payment.getMerchantUid())).willReturn(Optional.of(payment));
            given(portOneClient.getPaymentInfo(request.paymentId())).willReturn(response);

            assertThatThrownBy(() -> paymentService.verifyAndCompletePayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }
    }

    @Nested
    class 결제_취소 {
        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = Payment.createPayment(reservationGroup,
                    "테스터", "01012341234", "a@b.com",
                    "merchant-uid", BigDecimal.valueOf(reservationGroup.getTotalPrice())
            );

            payment.successPayment(
                    "payment-id", BigDecimal.valueOf(reservationGroup.getTotalPrice()),
                    "pg-provider", "pay-method", "card-name", 0,
                    LocalDateTime.of(2026, 11, 11, 0, 0)
            );
        }

        @Test
        void 결제_취소() {
            PortOneCancelRequest request = new PortOneCancelRequest(BigDecimal.valueOf(reservationGroup.getTotalPrice()), "사용자 예약 취소로 인한 환불");
            RefundResult result = new RefundResult(true, BigDecimal.valueOf(reservationGroup.getTotalPrice()), null);

            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));
            given(portOneClient.refundPayment(eq(payment.getPaymentId()), any(PortOneCancelRequest.class))).willReturn(result);

            paymentService.cancelPayment(reservationGroup, 2000);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getRefundAmount()).isEqualTo(request.amount());
            assertThat(payment.getCancelledAt()).isNotNull();
        }

        @Test
        void 결제_없음_404반환() {
            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.cancelPayment(reservationGroup, 2000))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 결제_완료_상태아님_409반환() {
            payment.failedPayment("failed_payment");

            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(reservationGroup, 2000))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 환불_예정_금액_0보다_작으면_409반환() {
            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPayment(reservationGroup, 2))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 환불_요청_중_실패_시_500반환() {
            RefundResult result = new RefundResult(false, BigDecimal.valueOf(reservationGroup.getTotalPrice()), "환불 중 오류");

            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));
            given(portOneClient.refundPayment(eq(payment.getPaymentId()), any(PortOneCancelRequest.class))).willReturn(result);

            assertThatThrownBy(() -> paymentService.cancelPayment(reservationGroup, 2000))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
    }

    @Nested
    class 상영_취소_환불 {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = Payment.createPayment(reservationGroup,
                    "테스터", "01012341234", "a@b.com",
                    "merchant-uid", BigDecimal.valueOf(reservationGroup.getTotalPrice())
            );

            payment.successPayment(
                    "payment-id", BigDecimal.valueOf(reservationGroup.getTotalPrice()),
                    "pg-provider", "pay-method", "card-name", 0,
                    LocalDateTime.of(2026, 11, 11, 0, 0)
            );
        }

        @Test
        void 환불_성공() {
            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));

            PortOneCancelRequest request = new PortOneCancelRequest(BigDecimal.valueOf(reservationGroup.getTotalPrice()), "사용자 예약 취소로 인한 환불");
            RefundResult result = new RefundResult(true, BigDecimal.valueOf(reservationGroup.getTotalPrice()), null);

            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));
            given(portOneClient.refundPayment(eq(payment.getPaymentId()), any(PortOneCancelRequest.class))).willReturn(result);

            paymentService.cancelPaymentByAdmin(reservationGroup);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getRefundAmount()).isEqualTo(request.amount());
            assertThat(payment.getCancelledAt()).isNotNull();
        }

        @Test
        void 결제_완료_상태아님_409반환() {
            payment.failedPayment("failed_payment");

            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.cancelPaymentByAdmin(reservationGroup))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        void 환불_요청_중_실패_시_500반환() {
            RefundResult result = new RefundResult(false, BigDecimal.valueOf(reservationGroup.getTotalPrice()), "환불 중 오류");

            given(paymentRepository.findByReservationGroupId(reservationGroup.getId()))
                    .willReturn(Optional.of(payment));
            given(portOneClient.refundPayment(eq(payment.getPaymentId()), any(PortOneCancelRequest.class))).willReturn(result);

            assertThatThrownBy(() -> paymentService.cancelPaymentByAdmin(reservationGroup))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

    }
}