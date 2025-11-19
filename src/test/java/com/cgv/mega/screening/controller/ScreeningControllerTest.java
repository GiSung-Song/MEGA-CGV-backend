package com.cgv.mega.screening.controller;

import com.cgv.mega.screening.dto.MovieScreeningResponse;
import com.cgv.mega.screening.dto.ScreeningDateMovieResponse;
import com.cgv.mega.screening.service.ScreeningService;
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

}