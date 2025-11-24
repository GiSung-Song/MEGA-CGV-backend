package com.cgv.mega.screening;

import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import com.cgv.mega.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
public class ScreeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    private Movie monster;
    private Movie interstellar;
    private Movie kingkong;
    private Screening screening;
    private Theater theater;

    @BeforeEach
    void setUp() {
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
    class 상영중인_영화_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            mockMvc.perform(get("/api/screenings/movies")
                            .param("date", "2026-11-11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.movieInfos[*].title", containsInAnyOrder("괴물", "인터스텔라", "킹콩")))
                    .andDo(document("screening-movie-list",
                            queryParameters(
                                    parameterWithName("date").description("조회할 날짜")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("HTTP 응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data.movieInfos[].movieId").description("영화 ID"),
                                    fieldWithPath("data.movieInfos[].title").description("영화 제목"),
                                    fieldWithPath("data.movieInfos[].posterUrl").description("영화 포스터 URL")
                            )
                    ))
                    .andDo(print());
        }

        @Test
        void 파라미터_타입_다름_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/screenings/movies")
                            .param("date", "20261111"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_영화의_상영회차_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            mockMvc.perform(get("/api/screenings/{movieId}", monster.getId())
                            .param("date", "2026-11-11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].startTime").value("2026-11-11T08:00:00"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].endTime").value("2026-11-11T10:30:00"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[1].startTime").value("2026-11-11T11:00:00"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[1].endTime").value("2026-11-11T13:30:00"))
                    .andDo(document("screening-movie-screening-list",
                            pathParameters(
                                    parameterWithName("movieId").description("조회할 영화의 ID")
                            ),
                            queryParameters(
                                    parameterWithName("date").description("조회할 날짜")
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
            mockMvc.perform(MockMvcRequestBuilders.get("/api/screenings/{movieId}", "abcdefg")
                            .param("date", "2026-11-11"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 파라미터_타입_다름_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/screenings/{movieId}", monster.getId())
                            .param("date", "20261111"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 상영회차별_좌석현황_조회 {
        @Test
        void 조회_성공() throws Exception {
            Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                            theater.getId(), "A", 3)
                    .orElseThrow();

            ScreeningSeat reservedSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                    .orElseThrow();

            reservedSeat.blockScreeningSeat();
            reservedSeat.reserveScreeningSeat();

            mockMvc.perform(get("/api/screenings/{screeningId}/seats", screening.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.screeningSeatInfos[0].rowLabel").value("A"))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[0].colNumber").value(1))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[0].status").value(ScreeningSeatStatus.AVAILABLE.name()))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[1].rowLabel").value("A"))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[1].colNumber").value(2))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[1].status").value(ScreeningSeatStatus.AVAILABLE.name()))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[2].rowLabel").value("A"))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[2].colNumber").value(3))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[2].status").value(ScreeningSeatStatus.RESERVED.name()))
                    .andDo(document("screening-seat-list",
                            pathParameters(
                                    parameterWithName("screeningId").description("조회할 상영회차의 식별자 ID")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("HTTP 응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data.screeningId").description("상영회차 식별자 ID"),
                                    fieldWithPath("data.basePrice").description("상영관별 좌석의 기본 가격"),
                                    fieldWithPath("data.screeningSeatInfos[].screeningSeatId").description("상영회차별 좌석 식별자 ID"),
                                    fieldWithPath("data.screeningSeatInfos[].rowLabel").description("행"),
                                    fieldWithPath("data.screeningSeatInfos[].colNumber").description("열"),
                                    fieldWithPath("data.screeningSeatInfos[].seatType").description("좌석 종류"),
                                    fieldWithPath("data.screeningSeatInfos[].status").description("좌석 상태")
                            )
                    ))
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/screenings/{screeningId}/seats", "1L"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }
}
