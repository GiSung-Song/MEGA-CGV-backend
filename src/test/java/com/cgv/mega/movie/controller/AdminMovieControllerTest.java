package com.cgv.mega.movie.controller;

import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.dto.MovieInfoResponse;
import com.cgv.mega.movie.dto.MovieListResponse;
import com.cgv.mega.movie.dto.RegisterMovieRequest;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.movie.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMovieController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    @Nested
    class 영화_등록 {
        @Test
        void 영화_등록_성공() throws Exception {
            RegisterMovieRequest request = new RegisterMovieRequest(
                    "인터스텔라", 150, "인터스텔라 설명", "poster.png",
                    Set.of(MovieType.TWO_D, MovieType.THREE_D), Set.of(1L, 2L, 3L));

            doNothing().when(movieService).registerMovie(any(RegisterMovieRequest.class));

            mockMvc.perform(post("/api/admin/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(print());
        }

        @Test
        void 필수값_누락_400반환() throws Exception {
            RegisterMovieRequest request = new RegisterMovieRequest(
                    "인터스텔라", 150, "인터스텔라 설명", "poster.png",
                    Set.of(MovieType.TWO_D, MovieType.THREE_D), Collections.emptySet());

            mockMvc.perform(post("/api/admin/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 영화_상세조회 {
        @Test
        void 영화_상세조회_성공() throws Exception {
            MovieInfoResponse response = new MovieInfoResponse(
                    "혹성탈출", 150, "혹성탈출 설명", "escape.jpg",
                    Set.of("ACTION", "DRAMA"), Set.of("2D", "3D")
            );

            given(movieService.getMovieInfoForAdmin(1L)).willReturn(response);

            mockMvc.perform(get("/api/admin/movies/{movieId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("혹성탈출"))
                    .andExpect(jsonPath("$.data.duration").value(150))
                    .andExpect(jsonPath("$.data.genres", containsInAnyOrder("ACTION", "DRAMA")))
                    .andExpect(jsonPath("$.data.types", containsInAnyOrder("2D", "3D")))
                    .andDo(print());
        }

        @Test
        void 경로변수_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/admin/movies/{movieId}", "fdsafeqfdsa"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 서비스에서_404반환() throws Exception {
            given(movieService.getMovieInfoForAdmin(1L)).willThrow(new CustomException(ErrorCode.MOVIE_NOT_FOUND));

            mockMvc.perform(get("/api/admin/movies/{movieId}", 1L))
                    .andExpect(status().isNotFound())
                    .andDo(print());
        }
    }

    @Nested
    class 영화_삭제 {
        @Test
        void 영화_삭제_성공() throws Exception {
            doNothing().when(movieService).deleteMovie(anyLong());

            mockMvc.perform(patch("/api/admin/movies/{movieId}", 1L))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        void 경로변수_오류_400반환() throws Exception {
            mockMvc.perform(patch("/api/admin/movies/{movieId}", "fdsafewq"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 영화_목록_조회 {
        @Test
        void 영화_목록_조회() throws Exception {
            List<MovieListResponse> content = List.of(
                    new MovieListResponse(1L, "혹성탈출", Set.of("ACTION", "DRAMA"), Set.of("TWO_D", "THREE_D"), "poster1.jpg"),
                    new MovieListResponse(2L, "인터스텔라", Set.of("DRAMA"), Set.of("TWO_D", "THREE_D"), "poster2.jpg")
            );

            PageResponse<MovieListResponse> pageResponse = new PageResponse<>(
                    content,
                    new PageResponse.PageInfo(0, 10, 1, 1, true)
            );

            given(movieService.getMovieList(eq("혹성"), any(Pageable.class))).willReturn(pageResponse);

            mockMvc.perform(get("/api/admin/movies")
                            .param("keyword", "혹성")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].title").value("혹성탈출"))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                    .andDo(print());
        }
    }
}