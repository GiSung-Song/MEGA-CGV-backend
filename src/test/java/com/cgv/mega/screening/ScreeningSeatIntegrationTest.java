package com.cgv.mega.screening;

import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.screening.dto.ScreeningSeatHoldDto;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ScreeningSeatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
    private Screening screening;
    private Theater theater;

    private User user;
    private String userToken;

    private ScreeningSeat screeningSeat1;
    private ScreeningSeat screeningSeat2;
    private ScreeningSeat screeningSeat3;

    @BeforeEach
    void setUp() {
        user = testDataFactory.createUser("user", "a@b.com", "01012345678");
        userToken = testDataFactory.setLogin(user);

        monster = testDataFactory.createMovie("괴물");

        theater = theaterRepository.findById(1L)
                .orElseThrow();

        LocalDateTime monsterStartTime = LocalDateTime.of(2026, 11, 11, 8, 0);
        screening = testDataFactory.createScreening(monster, theater, monsterStartTime, 1, MovieType.TWO_D);
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

        screeningSeat1 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat.getId())
                .orElseThrow();

        screeningSeat2 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat2.getId())
                .orElseThrow();

        screeningSeat3 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), seat3.getId())
                .orElseThrow();

    }

    @Nested
    class 좌석_홀드 {
        @Test
        void 좌석_홀드_성공() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(screeningSeat1.getId(), screeningSeat2.getId(), screeningSeat3.getId()));

            mockMvc.perform(post("/api/screenings/{screeningId}/seats/hold", screening.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))
                    .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andDo(document("screening-seat-hold",
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
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            for (Long screeningSeatId : req.screeningSeatIds()) {
                String key = "seat:" + screeningSeatId;

                Object value = redisTemplate.opsForValue().get(key);

                assertThat(value).isEqualTo(user.getId().toString());
            }
        }

        @Test
        void 비로그인_401반환() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(screeningSeat1.getId(), screeningSeat2.getId(), screeningSeat3.getId()));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/screenings/{screeningId}/seats/hold", screening.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    class 좌석_홀드_취소 {
        @Test
        void 홀드_취소_성공() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(screeningSeat1.getId(), screeningSeat2.getId(), screeningSeat3.getId()));

            for (Long screeningSeatId : req.screeningSeatIds()) {
                String key = "seat:" + screeningSeatId;

                redisTemplate.opsForValue().set(key, user.getId().toString(), Duration.ofMinutes(5));
            }

            mockMvc.perform(delete("/api/screenings/{screeningId}/seats/hold", screening.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andDo(document("screening-seat-hold-cancel",
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
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            for (Long screeningSeatId : req.screeningSeatIds()) {
                String key = "seat:" + screeningSeatId;

                Object value = redisTemplate.opsForValue().get(key);

                assertThat(value).isNull();
            }
        }

        @Test
        void 비로그인_401반환() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(screeningSeat1.getId(), screeningSeat2.getId(), screeningSeat3.getId()));

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/screenings/{screeningId}/seats/hold", screening.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}
