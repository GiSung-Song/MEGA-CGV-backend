package com.cgv.mega.screening.controller;

import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.screening.dto.AvailableScreeningResponse;
import com.cgv.mega.screening.dto.MovieScreeningForAdminResponse;
import com.cgv.mega.screening.dto.RegisterScreeningRequest;
import com.cgv.mega.screening.enums.ScreeningStatus;
import com.cgv.mega.screening.service.ScreeningSeatService;
import com.cgv.mega.screening.service.ScreeningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminScreeningController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminScreeningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScreeningService screeningService;

    @MockitoBean
    private ScreeningSeatService screeningSeatService;

    @Nested
    class 상영_가능_시간_조회 {
        @Test
        void 조회_성공() throws Exception {
            AvailableScreeningResponse response = new AvailableScreeningResponse(
                    List.of(LocalDateTime.of(2026, 11, 11, 8, 0),
                            LocalDateTime.of(2026, 11, 11, 8, 10),
                            LocalDateTime.of(2026, 11, 11, 8, 20),
                            LocalDateTime.of(2026, 11, 11, 8, 30))
            );

            given(screeningService.getAvailableScreeningTime(anyLong(), anyLong(), any()))
                    .willReturn(response);

            mockMvc.perform(get("/api/admin/screenings")
                            .param("movieId", "1")
                            .param("theaterId", "1")
                            .param("date", "2026-11-11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.availableTime[*]",
                            containsInAnyOrder(
                                    "2026-11-11T08:00:00",
                                    "2026-11-11T08:10:00",
                                    "2026-11-11T08:20:00",
                                    "2026-11-11T08:30:00")
                    ))
                    .andDo(print());
        }

        @Test
        void 파라미터_타입_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/admin/screenings")
                            .param("movieId", "1")
                            .param("theaterId", "abcdefg")
                            .param("date", "2026-11-11"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 상영_회차_등록 {
        @Test
        void 등록_성공() throws Exception {
            doNothing().when(screeningService).registerScreening(any(RegisterScreeningRequest.class));

            RegisterScreeningRequest request = new RegisterScreeningRequest(
                    1L, MovieType.TWO_D, 1L, LocalDateTime.of(2026, 11, 11, 8, 0)
            );

            mockMvc.perform(post("/api/admin/screenings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(print());
        }

        @Test
        void 필수값_누락_400반환() throws Exception {
            RegisterScreeningRequest request = new RegisterScreeningRequest(
                    1L, null, 1L, LocalDateTime.of(2026, 11, 11, 8, 0)
            );

            mockMvc.perform(post("/api/admin/screenings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_영화의_상영회차_목록_조회 {
        @Test
        void 조회_성공() throws Exception {
            MovieScreeningForAdminResponse response = new MovieScreeningForAdminResponse(
                    List.of(new MovieScreeningForAdminResponse.MovieScreeningInfo(
                            0L, 1L, "1관", Long.valueOf(50),
                            LocalDateTime.of(2026, 11, 11, 8, 0),
                            LocalDateTime.of(2026, 11, 11, 11, 0),
                            1, ScreeningStatus.SCHEDULED))
            );

            given(screeningService.getMovieScreeningsForAdmin(anyLong())).willReturn(response);

            mockMvc.perform(get("/api/admin/screenings/{movieId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].theaterName").value("1관"))
                    .andExpect(jsonPath("$.data.movieScreeningInfos[0].remainSeat").value("50"))
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/admin/screenings/{movieId}", "abcdefg"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_좌석_수리중_상태로_변경 {
        @Test
        void 변경_성공() throws Exception {
            willDoNothing().given(screeningSeatService).fixingScreeningSeat(anyLong());

            mockMvc.perform(patch("/api/admin/screenings/seats/{screeningSeatId}/fix", 1L))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(patch("/api/admin/screenings/seats/{screeningSeatId}/fix", "1L"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 특정_좌석_예약가능_상태로_변경 {
        @Test
        void 변경_성공() throws Exception {
            willDoNothing().given(screeningSeatService).restoringScreeningSeat(anyLong());

            mockMvc.perform(patch("/api/admin/screenings/seats/{screeningSeatId}/restore", 1L))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(patch("/api/admin/screenings/seats/{screeningSeatId}/restore", "1L"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 상영_취소 {
        @Test
        void 취소_성공() throws Exception {
            willDoNothing().given(screeningService).cancelScreening(anyLong());

            mockMvc.perform(delete("/api/admin/screenings/{screeningId}", 1L))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(delete("/api/admin/screenings/{screeningId}", "1L"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }
}