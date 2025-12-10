package com.cgv.mega.theater;

import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.seat.repository.SeatQueryRepository;
import com.cgv.mega.theater.repository.TheaterRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
public class TheaterIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private TheaterRepository theaterRepository;

    @MockitoSpyBean
    private SeatQueryRepository seatQueryRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    @Test
    void 상영관_목록_조회() throws Exception {
        mockMvc.perform(get("/api/theaters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theaterInfoList[0].theaterName").value("1관"))
                .andExpect(jsonPath("$.data.theaterInfoList[0].theaterType").value("2D관"))
                .andExpect(jsonPath("$.data.theaterInfoList[0].seatCount.일반").value(40))
                .andExpect(jsonPath("$.data.theaterInfoList[0].seatCount.프리미엄").value(7))
                .andExpect(jsonPath("$.data.theaterInfoList[0].seatCount.방").value(3))

                .andExpect(jsonPath("$.data.theaterInfoList[1].theaterName").value("2관"))
                .andExpect(jsonPath("$.data.theaterInfoList[1].theaterType").value("4DX관"))
                .andExpect(jsonPath("$.data.theaterInfoList[1].seatCount.일반").value(40))
                .andExpect(jsonPath("$.data.theaterInfoList[1].seatCount.프리미엄").value(7))
                .andExpect(jsonPath("$.data.theaterInfoList[1].seatCount.방").value(3))

                .andExpect(jsonPath("$.data.theaterInfoList[2].theaterName").value("3관"))
                .andExpect(jsonPath("$.data.theaterInfoList[2].theaterType").value("IMAX관"))
                .andExpect(jsonPath("$.data.theaterInfoList[2].seatCount.일반").value(20))
                .andExpect(jsonPath("$.data.theaterInfoList[2].seatCount.프리미엄").value(5))
                .andExpect(jsonPath("$.data.theaterInfoList[2].seatCount.방").value(5))

                .andExpect(jsonPath("$.data.theaterInfoList[3].theaterName").value("4관"))
                .andExpect(jsonPath("$.data.theaterInfoList[3].theaterType").value("SCREEN X관"))
                .andExpect(jsonPath("$.data.theaterInfoList[3].seatCount.일반").value(10))
                .andExpect(jsonPath("$.data.theaterInfoList[3].seatCount.프리미엄").value(5))
                .andExpect(jsonPath("$.data.theaterInfoList[3].seatCount.방").value(5))

                .andDo(
                        document("theater-list",
                                responseFields(
                                        fieldWithPath("status").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("data.theaterInfoList[].theaterId").description("상영관 ID"),
                                        fieldWithPath("data.theaterInfoList[].theaterName").description("상영관 이름"),
                                        fieldWithPath("data.theaterInfoList[].theaterType").description("상영관 타입"),
                                        fieldWithPath("data.theaterInfoList[].totalSeat").description("전체 좌석 수"),
                                        subsectionWithPath("data.theaterInfoList[].seatCount")
                                                .description("좌석 타입별 좌석 수 (key: 좌석 타입, value: 좌석 개수)")
                                )
                        ))

                .andDo(print());
    }

    @Test
    void 상영관_목록_조회_캐싱_적용() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/theaters"))
                .andExpect(status().isOk())
                .andDo(print());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/theaters"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(theaterRepository, times(1)).findAll(Sort.by(Sort.Direction.ASC, "id"));
        verify(seatQueryRepository, times(1)).getSeatCountGroupByTheater();
    }
}