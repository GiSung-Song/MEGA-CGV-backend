package com.cgv.mega.reservation.controller;

import com.cgv.mega.booking.dto.BookingResponse;
import com.cgv.mega.booking.service.BookingService;
import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.reservation.dto.ReservationDetailResponse;
import com.cgv.mega.reservation.dto.ReservationListResponse;
import com.cgv.mega.reservation.dto.ReservationRequest;
import com.cgv.mega.reservation.service.ReservationService;
import com.cgv.mega.util.CustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private BookingService bookingService;

    private Long userId = 1L;
    private Long screeningId = 50L;
    private Long reservationGroupId = 99L;
    private String paymentId = "payment-id";

    @Nested
    class 예약 {
        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 성공() throws Exception {
            BigDecimal expectedAmount = BigDecimal.valueOf(15000.00);

            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L, 3L));

            BookingResponse bookingresponse = new BookingResponse(
                    reservationGroupId, paymentId, expectedAmount
            );

            given(bookingService.startBooking(userId, screeningId, request)).willReturn(bookingresponse);

            mockMvc.perform(post("/api/reservations/{screeningId}", screeningId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.reservationGroupId").value(reservationGroupId.toString()))
                    .andExpect(jsonPath("$.data.paymentId").value(paymentId))
                    .andExpect(jsonPath("$.data.expectedAmount").value(expectedAmount.toString()))
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 필수값_누락_400반환() throws Exception {
            ReservationRequest request = new ReservationRequest(Collections.emptySet());

            mockMvc.perform(post("/api/reservations/{screeningId}", screeningId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 경로_변수_오류_400반환() throws Exception {
            ReservationRequest request = new ReservationRequest(Set.of(1L, 2L, 3L));

            mockMvc.perform(post("/api/reservations/{screeningId}", "screeningId")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 예약_목록_조회 {
        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 성공() throws Exception {
            Pageable pageable = PageRequest.of(0, 5);

            List<ReservationListResponse> content = List.of(
                    new ReservationListResponse(
                            reservationGroupId, "title", "2D",
                            LocalDateTime.of(2026, 11, 11, 10, 0),
                            "1관", "4DX",
                            List.of(new ReservationListResponse.SeatInfo("A2", "일반")),
                            "예약완료", 15000, "title.png", null
                    )
            );

            PageImpl<ReservationListResponse> page = new PageImpl<>(content, pageable, 1);

            PageResponse<ReservationListResponse> response = PageResponse.from(page);

            given(reservationService.getReservationList(userId, pageable)).willReturn(response);

            mockMvc.perform(get("/api/reservations")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("title"))
                    .andExpect(jsonPath("$.data.content[0].theaterName").value("1관"))
                    .andExpect(jsonPath("$.data.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.size").value(5))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                    .andDo(print());
        }
    }

    @Nested
    class 예약_취소 {
        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 성공() throws Exception {
            willDoNothing().given(reservationService).cancelReservation(userId, reservationGroupId);

            mockMvc.perform(delete("/api/reservations/{reservationGroupId}", reservationGroupId))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(delete("/api/reservations/{reservationGroupId}", "reservationGroupId"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 예약_상세_조회 {
        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 성공() throws Exception {
            ReservationDetailResponse response = new ReservationDetailResponse(
                    1L, "title", "2D", "poster.png", 150, 99L,
                    LocalDateTime.of(2026, 11, 11, 10, 0),
                    LocalDateTime.of(2026, 11, 11, 12, 30),
                    222L, "1관", "IMAX",
                    List.of(new ReservationDetailResponse.SeatInfo("A1", "NORMAL")),
                    reservationGroupId, "PAID",
                    LocalDateTime.of(2026, 11, 11, 8, 0),
                    null, "COMPLETED", "CARD", BigDecimal.valueOf(15000.00),
                    null, "payment-id",
                    "buyer", "01012341234", "a@b.com"
            );

            given(reservationService.getReservationDetail(1L, reservationGroupId)).willReturn(response);

            mockMvc.perform(get("/api/reservations/{reservationGroupId}", reservationGroupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("title"))
                    .andExpect(jsonPath("$.data.theaterName").value("1관"))
                    .andExpect(jsonPath("$.data.buyerPhoneNumber").value("01012341234"))
                    .andExpect(jsonPath("$.data.paymentAmount").value(BigDecimal.valueOf(15000.00).toString()))
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 경로_변수_오류_400반환() throws Exception {
            mockMvc.perform(get("/api/reservations/{reservationGroupId}", "reservationGroupId"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }
}