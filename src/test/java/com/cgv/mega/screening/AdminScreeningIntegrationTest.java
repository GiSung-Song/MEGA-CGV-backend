package com.cgv.mega.screening;

import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.screening.dto.RegisterScreeningRequest;
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
import com.cgv.mega.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
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
@Transactional
@ActiveProfiles("test")
public class AdminScreeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    private String userToken;
    private String adminToken;

    private Movie monster;
    private Movie interstellar;
    private Movie kingkong;
    private Screening screening;
    private Theater theater;

    @BeforeEach
    void setUp() {
        User user = testDataFactory.createUser("user", "a@b.com", "01012345678");
        User admin = testDataFactory.createUser("admin", "c@d.com", "01098765432");

        testDataFactory.setAdmin(admin);

        userToken = testDataFactory.setLogin(user);
        adminToken = testDataFactory.setLogin(admin);

        monster = testDataFactory.createMovie("괴물");
        interstellar = testDataFactory.createMovie("인터스텔라");
        kingkong = testDataFactory.createMovie("킹콩");

        theater = theaterRepository.findById(1L)
                .orElseThrow();

        LocalDateTime monsterStartTime1 = LocalDateTime.of(2026, 11, 11, 8, 0);
        LocalDateTime monsterStartTime2 = LocalDateTime.of(2026, 11, 11, 11, 0);
        LocalDateTime interstellarStartTime = LocalDateTime.of(2026, 11, 11, 20, 0);
        LocalDateTime kingkongStartTime = LocalDateTime.of(2026, 11, 11, 23, 0);

        screening = testDataFactory.createScreening(monster, theater, monsterStartTime1, 1);

        testDataFactory.initializeScreeningSeat(screening, theater);

        testDataFactory.createScreening(monster, theater, monsterStartTime2, 2);
        testDataFactory.createScreening(interstellar, theater, interstellarStartTime, 1);
        testDataFactory.createScreening(kingkong, theater, kingkongStartTime, 1);
    }

    @Nested
    class 상영_가능_시간_조회 {
        @Test
        void 조회_성공() throws Exception {
            mockMvc.perform(get("/api/admin/screenings")
                            .param("movieId", monster.getId().toString())
                            .param("theaterId", theater.getId().toString())
                            .param("date", "2026-11-11")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.availableTime[*]",
                            hasItems(
                                    "2026-11-11T05:00:00",
                                    "2026-11-11T05:10:00",
                                    "2026-11-11T13:40:00",
                                    "2026-11-11T13:50:00")
                    ))
                    .andDo(document("screening-register-available-time-list",
                            queryParameters(
                                    parameterWithName("movieId").description("영화 식별자 ID"),
                                    parameterWithName("theaterId").description("상영관 식별자 ID"),
                                    parameterWithName("date").description("조회할 날짜")
                            ),
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data.availableTime[]").description("상영 등록 가능한 시간들")
                            )
                    ))
                    .andDo(print());
        }

        @Test
        void 파라미터_타입_오류_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/screenings")
                            .param("movieId", "1")
                            .param("theaterId", "abcdefg")
                            .param("date", "2026-11-11")
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 권한없음_403반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/screenings")
                            .param("movieId", monster.getId().toString())
                            .param("theaterId", theater.getId().toString())
                            .param("date", "2026-11-11")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Nested
    class 상영_회차_등록 {
        @Test
        void 등록_성공() throws Exception {
            RegisterScreeningRequest request = new RegisterScreeningRequest(
                    interstellar.getId(), MovieType.TWO_D, theater.getId(),
                    LocalDateTime.of(2026, 11, 11, 14, 20)
            );

            mockMvc.perform(post("/api/admin/screenings")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(document("screening-register",
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            requestFields(
                                    fieldWithPath("movieId").description("영화 식별자 ID"),
                                    fieldWithPath("movieType").description("영화 타입(2D, 3D)"),
                                    fieldWithPath("theaterId").description("상영관 식별자 ID"),
                                    fieldWithPath("startTime").description("상영 시작 시간")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            int size = screeningRepository.findAll()
                    .size();

            assertThat(size).isEqualTo(5);
        }

        @Test
        void 영화_등록_불가능한_시간_409반환() throws Exception {
            RegisterScreeningRequest request = new RegisterScreeningRequest(
                    interstellar.getId(), MovieType.TWO_D, theater.getId(),
                    LocalDateTime.of(2026, 11, 11, 8, 20)
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/screenings")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andDo(print());
        }

        @Test
        void 필수값_누락_400반환() throws Exception {
            RegisterScreeningRequest request = new RegisterScreeningRequest(
                    monster.getId(), null, theater.getId(), LocalDateTime.of(2026, 11, 11, 8, 0)
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/screenings")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 권한_없음_403반환() throws Exception {
            RegisterScreeningRequest request = new RegisterScreeningRequest(
                    interstellar.getId(), MovieType.TWO_D, theater.getId(),
                    LocalDateTime.of(2026, 11, 11, 14, 20)
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/screenings")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_영화의_상영회차_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            mockMvc.perform(get("/api/admin/screenings/{movieId}", monster.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].startTime").value("2026-11-11T08:00:00"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].endTime").value("2026-11-11T10:30:00"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[1].startTime").value("2026-11-11T11:00:00"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[1].endTime").value("2026-11-11T13:30:00"))
                    .andDo(document("screening-movie-screening-list-admin",
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            pathParameters(
                                    parameterWithName("movieId").description("조회할 영화의 ID")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("HTTP 응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data.movieScreeningInfos[].screeningId").description("상영 식별자 ID"),
                                    fieldWithPath("data.movieScreeningInfos[].theaterId").description("상영관 식별자 ID"),
                                    fieldWithPath("data.movieScreeningInfos[].theaterName").description("상영관 이름"),
                                    fieldWithPath("data.movieScreeningInfos[].remainSeat").description("남은 좌석 수"),
                                    fieldWithPath("data.movieScreeningInfos[].startTime").description("영화 시작 시간"),
                                    fieldWithPath("data.movieScreeningInfos[].endTime").description("영화 종료 시간"),
                                    fieldWithPath("data.movieScreeningInfos[].sequence").description("영화 상영 회차")
                            )
                    ))
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/screenings/{movieId}", "abcdefg")
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 권한_없음_403반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/screenings/{movieId}", monster.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class 특정_좌석_수리중_상태로_변경 {
        @Test
        void 변경_성공() throws Exception {
            Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                            theater.getId(), "A", 3)
                    .orElseThrow();

            ScreeningSeat screeningSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            mockMvc.perform(patch("/api/admin/screenings/seats/{screeningSeatId}/fix", screeningSeat.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andDo(document("screening-seat-fixing",
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            pathParameters(
                                    parameterWithName("screeningSeatId").description("상영회차별 좌석 식별자 ID")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("HTTP 응답 코드"),
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            ScreeningSeat brokenSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            assertThat(brokenSeat.getStatus()).isEqualTo(ScreeningSeatStatus.FIXING);
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/screenings/seats/{screeningSeatId}/fix", "1L")
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 권한_없음_403반환() throws Exception {
            Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                            theater.getId(), "A", 3)
                    .orElseThrow();

            ScreeningSeat screeningSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/screenings/seats/{screeningSeatId}/fix", screeningSeat.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_좌석_예약가능_상태로_변경 {
        @Test
        void 변경_성공() throws Exception {
            Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                            theater.getId(), "A", 3)
                    .orElseThrow();

            ScreeningSeat screeningSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            screeningSeat.fixScreeningSeat();

            mockMvc.perform(patch("/api/admin/screenings/seats/{screeningSeatId}/restore", screeningSeat.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andDo(document("screening-seat-fixing",
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            pathParameters(
                                    parameterWithName("screeningSeatId").description("상영회차별 좌석 식별자 ID")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("HTTP 응답 코드"),
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            ScreeningSeat availableSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            assertThat(availableSeat.getStatus()).isEqualTo(ScreeningSeatStatus.AVAILABLE);
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/screenings/seats/{screeningSeatId}/restore", "1L")
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 권한_없음_403반환() throws Exception {
            Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                            theater.getId(), "A", 3)
                    .orElseThrow();

            ScreeningSeat screeningSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/screenings/seats/{screeningSeatId}/restore", screeningSeat.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }
}
