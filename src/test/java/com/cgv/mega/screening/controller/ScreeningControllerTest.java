package com.cgv.mega.screening.controller;

import com.cgv.mega.screening.dto.MovieScreeningResponse;
import com.cgv.mega.screening.dto.ScreeningDateMovieResponse;
import com.cgv.mega.screening.dto.ScreeningSeatResponse;
import com.cgv.mega.screening.enums.DisplayScreeningSeatStatus;
import com.cgv.mega.screening.service.ScreeningService;
import com.cgv.mega.seat.enums.SeatType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScreeningController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScreeningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScreeningService screeningService;

    @Nested
    class 상영중인_영화_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            ScreeningDateMovieResponse response = new ScreeningDateMovieResponse(
                    List.of(new ScreeningDateMovieResponse.MovieInfo(0L, "괴물", "monster.png"),
                            new ScreeningDateMovieResponse.MovieInfo(1L, "인터스텔라", "inter.jpg"),
                            new ScreeningDateMovieResponse.MovieInfo(2L, "킹콩", "kingkong@jpeg"))
            );

            given(screeningService.getScreeningMovies(any())).willReturn(response);

            mockMvc.perform(get("/api/screenings/movies")
                            .param("date", "2026-11-11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.movieInfos[*].title", containsInAnyOrder("괴물", "인터스텔라", "킹콩")))
                    .andDo(print());
        }

        @Test
        void 파라미터_타입_다름_400반환() throws Exception {
            mockMvc.perform(get("/api/screenings/movies")
                            .param("date", "20261111"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_영화의_상영회차_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            MovieScreeningResponse response = new MovieScreeningResponse(
                    List.of(new MovieScreeningResponse.MovieScreeningInfo(
                            0L, 1L, "1관", Long.valueOf(50),
                            LocalDateTime.of(2026, 11, 11, 8, 0),
                            LocalDateTime.of(2026, 11, 11, 11, 0),
                            1))
            );

            given(screeningService.getMovieScreenings(anyLong(), any())).willReturn(response);

            mockMvc.perform(get("/api/screenings/{movieId}", 1L)
                            .param("date", "2026-11-11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].theaterName").value("1관"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].remainSeat").value("50"))
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/screenings/{movieId}", "abcdefg")
                            .param("date", "2026-11-11"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 파라미터_타입_다름_400반환() throws Exception {
            mockMvc.perform(get("/api/screenings/{movieId}", 1L)
                            .param("date", "20261111"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 상영회차별_좌석현황_조회 {
        @Test
        void 조회_성공() throws Exception {
            ScreeningSeatResponse response = new ScreeningSeatResponse(
                    1L,
                    List.of(new ScreeningSeatResponse.ScreeningSeatInfo(
                                    1L, "A", 1, SeatType.NORMAL, DisplayScreeningSeatStatus.AVAILABLE, 15000),
                            new ScreeningSeatResponse.ScreeningSeatInfo(2L, "A", 2, SeatType.NORMAL, DisplayScreeningSeatStatus.FIXING, 15000),
                            new ScreeningSeatResponse.ScreeningSeatInfo(3L, "A", 3, SeatType.NORMAL, DisplayScreeningSeatStatus.HOLD, 15000)
                    ));

            given(screeningService.getScreeningSeatStatus(1L)).willReturn(response);

            mockMvc.perform(get("/api/screenings/{screeningId}/seats", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.screeningSeatInfos[0].rowLabel").value("A"))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[0].colNumber").value(1))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[0].status").value(DisplayScreeningSeatStatus.AVAILABLE.name()))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[1].rowLabel").value("A"))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[1].colNumber").value(2))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[1].status").value(DisplayScreeningSeatStatus.FIXING.name()))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[2].rowLabel").value("A"))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[2].colNumber").value(3))
                    .andExpect(jsonPath("$.data.screeningSeatInfos[2].status").value(DisplayScreeningSeatStatus.HOLD.name()))
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/screenings/{screeningId}/seats", "1L"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }


}