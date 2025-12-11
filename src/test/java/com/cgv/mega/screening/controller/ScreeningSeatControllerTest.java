package com.cgv.mega.screening.controller;

import com.cgv.mega.common.enums.Role;
import com.cgv.mega.screening.dto.ScreeningSeatHoldDto;
import com.cgv.mega.screening.service.ScreeningSeatService;
import com.cgv.mega.util.CustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Set;

import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScreeningSeatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScreeningSeatControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScreeningSeatService screeningSeatService;

    @Nested
    class 좌석_홀드 {
        @Test
        @CustomMockUser(id = 1L, name = "테스터", email = "a@b.com", role = Role.USER)
        void 홀드_성공() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(1L, 2L, 3L));

            willDoNothing().given(screeningSeatService).holdScreeningSeat(1L, 1L, req);

            mockMvc.perform(post("/api/screenings/{screeningId}/seats/hold", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "테스터", email = "a@b.com", role = Role.USER)
        void 경로_변수_오류_400반환() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(1L, 2L, 3L));

            mockMvc.perform(post("/api/screenings/{screeningId}/seats/hold", "1L")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "테스터", email = "a@b.com", role = Role.USER)
        void 필수값_누락_400반환() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Collections.emptySet());

            mockMvc.perform(post("/api/screenings/{screeningId}/seats/hold", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 좌석_홀드_취소 {
        @Test
        @CustomMockUser(id = 1L, name = "테스터", email = "a@b.com", role = Role.USER)
        void 홀드_성공() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(1L, 2L, 3L));

            willDoNothing().given(screeningSeatService).cancelHoldScreeningSeat(1L, req);

            mockMvc.perform(delete("/api/screenings/{screeningId}/seats/hold", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "테스터", email = "a@b.com", role = Role.USER)
        void 경로_변수_오류_400반환() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Set.of(1L, 2L, 3L));

            mockMvc.perform(delete("/api/screenings/{screeningId}/seats/hold", "1L")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "테스터", email = "a@b.com", role = Role.USER)
        void 필수값_누락_400반환() throws Exception {
            ScreeningSeatHoldDto req = new ScreeningSeatHoldDto(Collections.emptySet());

            mockMvc.perform(delete("/api/screenings/{screeningId}/seats/hold", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }
}