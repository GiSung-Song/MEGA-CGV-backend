package com.cgv.mega.movie.controller;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.dto.MovieInfoResponse;
import com.cgv.mega.movie.service.MovieService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Nested
    class 영화_상세조회 {
        @Test
        void 영화_상세조회_성공() throws Exception {
            MovieInfoResponse response = new MovieInfoResponse(
                    "혹성탈출", 150, "혹성탈출 설명", "escape.jpg",
                    Set.of("ACTION", "DRAMA"), Set.of("2D", "3D")
            );

            given(movieService.getMovieInfo(1L)).willReturn(response);

            mockMvc.perform(get("/api/movies/{movieId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("혹성탈출"))
                    .andExpect(jsonPath("$.data.duration").value(150))
                    .andExpect(jsonPath("$.data.genres", containsInAnyOrder("ACTION", "DRAMA")))
                    .andExpect(jsonPath("$.data.types", containsInAnyOrder("2D", "3D")))
                    .andDo(print());
        }

        @Test
        void 경로변수_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/movies/{movieId}", "fdsafeqfdsa"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 서비스에서_404반환() throws Exception {
            given(movieService.getMovieInfo(1L)).willThrow(new CustomException(ErrorCode.MOVIE_NOT_FOUND));

            mockMvc.perform(get("/api/movies/{movieId}", 1L))
                    .andExpect(status().isNotFound())
                    .andDo(print());
        }
    }
}