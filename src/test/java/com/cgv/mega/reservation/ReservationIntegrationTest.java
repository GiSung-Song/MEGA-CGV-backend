package com.cgv.mega.reservation;

import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.payment.dto.PortOneCancelRequest;
import com.cgv.mega.payment.dto.RefundResult;
import com.cgv.mega.payment.entity.Payment;
import com.cgv.mega.payment.enums.PaymentStatus;
import com.cgv.mega.payment.repository.PaymentRepository;
import com.cgv.mega.payment.service.PortOneClient;
import com.cgv.mega.reservation.dto.ReservationRequest;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.enums.ReservationStatus;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import com.cgv.mega.reservation.repository.ReservationRepository;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import com.cgv.mega.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
public class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationGroupRepository reservationGroupRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @MockitoBean
    private PortOneClient portOneClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    private User user;
    private String userToken;

    private Movie movie;
    private Theater theater;
    private Screening screening;

    private ScreeningSeat ss1;
    private ScreeningSeat ss2;
    private ScreeningSeat ss3;

    @BeforeEach
    void setUp() {
        user = testDataFactory.createUser("user", "a@b.com", "01012345678");
        userToken = testDataFactory.setLogin(user);

        movie = testDataFactory.createMovie("괴물");

        theater = theaterRepository.findById(1L)
                .orElseThrow();

        LocalDateTime startTime = LocalDateTime.of(2026, 11, 11, 8, 0);
        screening = testDataFactory.createScreening(movie, theater, startTime, 1, MovieType.TWO_D);
        testDataFactory.initializeScreeningSeat(screening, theater);

        Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                        theater.getId(), "A", 1)
                .orElseThrow();

        Seat seat2 = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                        theater.getId(), "A", 2)
                .orElseThrow();

        Seat seat3 = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                        theater.getId(), "A", 3)
                .orElseThrow();

        ss1 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                .orElseThrow();

        ss2 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat2.getId())
                .orElseThrow();

        ss3 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat3.getId())
                .orElseThrow();
    }

    @Nested
    class 영화_예약 {
        @Test
        @Commit
        void 예약_성공() throws Exception {
            // 예약 홀드 가정
            redisTemplate.opsForValue().set("seat:" + ss1.getId(), user.getId(), Duration.ofMinutes(5));
            redisTemplate.opsForValue().set("seat:" + ss2.getId(), user.getId(), Duration.ofMinutes(5));

            ReservationRequest request = new ReservationRequest(Set.of(ss1.getId(), ss2.getId()));

            mockMvc.perform(post("/api/reservations/{screeningId}", screening.getId())
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.reservationGroupId").exists())
                    .andExpect(jsonPath("$.data.merchantUid").exists())
                    .andExpect(jsonPath("$.data.expectedAmount").value(2000))
                    .andDo(
                            document("reservation-reserve",
                                    pathParameters(
                                            parameterWithName("screeningId").description("상영 회차 식별자 ID")
                                    ),
                                    requestHeaders(
                                            headerWithName("Authorization").description("JWT Access Token (Bearer)")
                                    ),
                                    requestFields(
                                            fieldWithPath("screeningSeatIds").description("상영회차별 좌석 식별자 IDs")
                                    ),
                                    responseFields(
                                            fieldWithPath("status").description("응답 코드"),
                                            fieldWithPath("message").description("응답 메시지"),
                                            fieldWithPath("data.reservationGroupId").description("예약 식별자 ID"),
                                            fieldWithPath("data.merchantUid").description("서비스 내 거래 고유번호"),
                                            fieldWithPath("data.expectedAmount").description("결제 예상 금액")
                                    )
                            ))
                    .andDo(print());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        Object redisSeat1 = redisTemplate.opsForValue().get("seat:" + ss1.getId());
                        Object redisSeat2 = redisTemplate.opsForValue().get("seat:" + ss2.getId());

                        assertThat(redisSeat1).isNull();
                        assertThat(redisSeat2).isNull();
                    });

            assertThat(reservationGroupRepository.findAll()).hasSize(1);
            assertThat(reservationRepository.findAll()).hasSize(2);

            assertThat(paymentRepository.findAll()).hasSize(1);
        }

        @Test
        @Transactional
        void 비로그인_401반환() throws Exception {
            redisTemplate.opsForValue().set("seat:" + ss1.getId(), user.getId(), Duration.ofMinutes(5));
            redisTemplate.opsForValue().set("seat:" + ss2.getId(), user.getId(), Duration.ofMinutes(5));

            ReservationRequest request = new ReservationRequest(Set.of(ss1.getId(), ss2.getId()));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/reservations/{screeningId}", screening.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @AfterEach
        void clear() {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("TRUNCATE TABLE payments");
            jdbcTemplate.execute("TRUNCATE TABLE reservations");
            jdbcTemplate.execute("TRUNCATE TABLE reservation_groups");
            jdbcTemplate.execute("TRUNCATE TABLE screening_seats");
            jdbcTemplate.execute("TRUNCATE TABLE screenings");
            jdbcTemplate.execute("TRUNCATE TABLE movies");
            jdbcTemplate.execute("TRUNCATE TABLE users");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    @Nested
    @Transactional
    class 예약_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            ReservationGroup reservationGroup1 = ReservationGroup.createReservationGroup(user.getId());
            reservationGroup1.addReservation(ss1);
            reservationGroup1.addReservation(ss2);

            ReservationGroup reservationGroup2 = ReservationGroup.createReservationGroup(user.getId());
            reservationGroup2.addReservation(ss3);
            reservationGroup2.successReservation();

            reservationGroupRepository.saveAll(List.of(reservationGroup1, reservationGroup2));

            mockMvc.perform(get("/api/reservations")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].reservationGroupId",
                            containsInAnyOrder(reservationGroup1.getId().intValue(), reservationGroup2.getId().intValue())))
                    .andExpect(jsonPath("$.data.content[*].title",
                            containsInAnyOrder(movie.getTitle(), movie.getTitle())))
                    .andExpect(jsonPath("$.data.content[*].seats[*].seatNumber",
                            containsInAnyOrder("A1", "A2", "A3")))
                    .andDo(
                            document("reservation-reserve-list",
                                    requestHeaders(
                                            headerWithName("Authorization").description("JWT Access Token (Bearer)")
                                    ),
                                    queryParameters(
                                            parameterWithName("page").description("페이지 번호(기본 0)").optional(),
                                            parameterWithName("size").description("한 페이지 크기(기본 10)").optional()
                                    ),
                                    responseFields(
                                            fieldWithPath("status").description("응답 코드"),
                                            fieldWithPath("message").description("응답 메시지"),
                                            fieldWithPath("data.content[].reservationGroupId").description("예약-그룹 식별자 ID"),
                                            fieldWithPath("data.content[].title").description("영화 제목"),
                                            fieldWithPath("data.content[].movieType").description("영화 타입(2D, 3D)"),
                                            fieldWithPath("data.content[].startTime").description("시작 시간"),
                                            fieldWithPath("data.content[].theaterName").description("상영관 이름"),
                                            fieldWithPath("data.content[].theaterType").description("상영관 타입(4DX, IMAX 등)"),
                                            fieldWithPath("data.content[].seats[].seatNumber").description("좌석 번호"),
                                            fieldWithPath("data.content[].seats[].seatType").description("좌석 타입(NORMAL, PREMIUM, ROOM)"),
                                            fieldWithPath("data.content[].reservationStatus").description("예약 상태"),
                                            fieldWithPath("data.content[].totalPrice").description("총 가격"),
                                            fieldWithPath("data.content[].posterUrl").description("포스터 이미지 URL"),
                                            fieldWithPath("data.content[].updatedAt").description("예약 상태 변경 시간"),
                                            fieldWithPath("data.pageInfo.page").description("현재 페이지 번호"),
                                            fieldWithPath("data.pageInfo.size").description("페이지 크기"),
                                            fieldWithPath("data.pageInfo.totalElements").description("총 검색 결과 수"),
                                            fieldWithPath("data.pageInfo.totalPages").description("총 페이지 수"),
                                            fieldWithPath("data.pageInfo.last").description("마지막 페이지 여부")
                                    )
                            ))
                    .andDo(print());
        }

        @Test
        void 비로그인_401반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/reservations"))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @Transactional
    class 예약_취소 {
        @Test
        void 취소_환불_mocking_성공() throws Exception {
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(user.getId());
            reservationGroup.addReservation(ss1);
            reservationGroup.addReservation(ss2);

            reservationGroup.successReservation();

            reservationGroupRepository.save(reservationGroup);

            ss1.reserveScreeningSeat();
            ss2.reserveScreeningSeat();

            assertThat(ss1.getStatus()).isEqualTo(ScreeningSeatStatus.RESERVED);
            assertThat(ss2.getStatus()).isEqualTo(ScreeningSeatStatus.RESERVED);

            Payment payment = Payment.createPayment(
                    reservationGroup, user.getName(), user.getPhoneNumber(),
                    user.getEmail(), "merchant-uid", BigDecimal.valueOf(2000)
            );

            payment.successPayment(
                    "payment-id",
                    BigDecimal.valueOf(2000),
                    "pg-provider",
                    "pay-method",
                    "card-name",
                    0,
                    LocalDateTime.now()
            );

            paymentRepository.save(payment);

            RefundResult result = new RefundResult(true, BigDecimal.valueOf(2000), null);
            given(portOneClient.refundPayment(anyString(), any(PortOneCancelRequest.class))).willReturn(result);

            mockMvc.perform(delete("/api/reservations/{reservationGroupId}", reservationGroup.getId())
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andDo(
                            document("reservation-reserve-cancel",
                                    pathParameters(
                                            parameterWithName("reservationGroupId").description("예약-그룹 식별자 ID")
                                    ),
                                    requestHeaders(
                                            headerWithName("Authorization").description("JWT Access Token (Bearer)")
                                    ),
                                    responseFields(
                                            fieldWithPath("status").description("응답 코드"),
                                            fieldWithPath("message").description("응답 메시지")
                                    )
                            ))
                    .andDo(print());

            ReservationGroup findRg = reservationGroupRepository.findById(reservationGroup.getId())
                    .orElseThrow();

            Payment findP = paymentRepository.findById(payment.getId())
                    .orElseThrow();

            ScreeningSeat seat1 = screeningSeatRepository.findById(ss1.getId()).orElseThrow();
            ScreeningSeat seat2 = screeningSeatRepository.findById(ss2.getId()).orElseThrow();

            assertThat(findRg.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(findP.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(findP.getRefundAmount()).isEqualTo(BigDecimal.valueOf(findRg.getTotalPrice()));
            assertThat(findP.getCancelReason()).isNotNull();

            assertThat(seat1.getStatus()).isEqualTo(ScreeningSeatStatus.AVAILABLE);
            assertThat(seat2.getStatus()).isEqualTo(ScreeningSeatStatus.AVAILABLE);
        }

        @Test
        void 비로그인_401반환() throws Exception {
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(user.getId());
            reservationGroup.addReservation(ss1);
            reservationGroup.addReservation(ss2);

            reservationGroupRepository.save(reservationGroup);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/reservations/{reservationGroupId}", reservationGroup.getId()))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @Transactional
    class 예약_상세_조회 {
        @Test
        void 조회_성공() throws Exception {
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(user.getId());
            reservationGroup.addReservation(ss1);
            reservationGroup.addReservation(ss2);

            reservationGroup.successReservation();

            reservationGroupRepository.save(reservationGroup);

            ss1.reserveScreeningSeat();
            ss2.reserveScreeningSeat();

            Payment payment = Payment.createPayment(
                    reservationGroup, user.getName(), user.getPhoneNumber(),
                    user.getEmail(), "merchant-uid", BigDecimal.valueOf(2000)
            );

            payment.successPayment(
                    "payment-id",
                    BigDecimal.valueOf(2000),
                    "pg-provider",
                    "pay-method",
                    "card-name",
                    0,
                    LocalDateTime.now()
            );

            paymentRepository.save(payment);

            mockMvc.perform(get("/api/reservations/{reservationGroupId}", reservationGroup.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value(movie.getTitle()))
                    .andExpect(jsonPath("$.data.theaterName").value(theater.getName()))
                    .andExpect(jsonPath("$.data.paymentAmount").value(2000))
                    .andExpect(jsonPath("$.data.buyerPhoneNumber").value(user.getPhoneNumber()))
                    .andDo(
                            document("reservation-reserve-detail",
                                    pathParameters(
                                            parameterWithName("reservationGroupId").description("예약-그룹 식별자 ID")
                                    ),
                                    requestHeaders(
                                            headerWithName("Authorization").description("JWT Access Token (Bearer)")
                                    ),
                                    responseFields(
                                            fieldWithPath("status").description("응답 코드"),
                                            fieldWithPath("message").description("응답 메시지"),
                                            fieldWithPath("data.movieId").description("영화 식별자 ID"),
                                            fieldWithPath("data.title").description("영화 제목"),
                                            fieldWithPath("data.movieType").description("영화 타입(2D, 3D)"),
                                            fieldWithPath("data.posterUrl").description("영화 이미지 URL"),
                                            fieldWithPath("data.duration").description("러닝타임"),
                                            fieldWithPath("data.screeningId").description("상영 회차 식별자 ID"),
                                            fieldWithPath("data.startTime").description("상영 시작 시간"),
                                            fieldWithPath("data.endTime").description("상영 종료 시간"),
                                            fieldWithPath("data.theaterId").description("상영관 식별자 ID"),
                                            fieldWithPath("data.theaterName").description("상영관 이름"),
                                            fieldWithPath("data.theaterType").description("상영관 타입(4DX, IMAX 등)"),
                                            fieldWithPath("data.seatInfos[].seatNumber").description("좌석 번호"),
                                            fieldWithPath("data.seatInfos[].seatType").description("좌석 타입(NORMAL, PREMIUM, ROOM)"),
                                            fieldWithPath("data.reservationGroupId").description("예약-그룹 식별자 ID"),
                                            fieldWithPath("data.reservationStatus").description("예약 상태"),
                                            fieldWithPath("data.reservationCreatedAt").description("예약 생성 시각"),
                                            fieldWithPath("data.reservationCancelledAt").description("예약 취소 시각"),
                                            fieldWithPath("data.paymentStatus").description("결제 상태"),
                                            fieldWithPath("data.paymentMethod").description("결제 방법"),
                                            fieldWithPath("data.paymentAmount").description("결제 금액"),
                                            fieldWithPath("data.refundAmount").description("환불 금액"),
                                            fieldWithPath("data.merchantUid").description("서비스 내 거래 고유번호"),
                                            fieldWithPath("data.paymentId").description("거래 고유번호"),
                                            fieldWithPath("data.buyerName").description("결제자 이름"),
                                            fieldWithPath("data.buyerPhoneNumber").description("결제자 휴대폰 번호"),
                                            fieldWithPath("data.buyerEmail").description("결제자 이메일")
                                    )
                            ))
                    .andDo(print());
        }

        @Test
        void 비로그인_401반환() throws Exception {
            ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(user.getId());
            reservationGroup.addReservation(ss1);
            reservationGroup.addReservation(ss2);

            reservationGroup.successReservation();

            reservationGroupRepository.save(reservationGroup);

            ss1.reserveScreeningSeat();
            ss2.reserveScreeningSeat();

            Payment payment = Payment.createPayment(
                    reservationGroup, user.getName(), user.getPhoneNumber(),
                    user.getEmail(), "merchant-uid", BigDecimal.valueOf(2000)
            );

            payment.successPayment(
                    "payment-id",
                    BigDecimal.valueOf(2000),
                    "pg-provider",
                    "pay-method",
                    "card-name",
                    0,
                    LocalDateTime.now()
            );

            paymentRepository.save(payment);

            mockMvc.perform(MockMvcRequestBuilders.get("/api/reservations/{reservationGroupId}", reservationGroup.getId()))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}
