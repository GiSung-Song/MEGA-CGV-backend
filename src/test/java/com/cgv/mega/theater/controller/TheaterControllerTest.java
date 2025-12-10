package com.cgv.mega.theater.controller;

import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.theater.dto.TheaterListResponse;
import com.cgv.mega.theater.enums.TheaterType;
import com.cgv.mega.theater.service.TheaterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.EnumMap;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TheaterController.class)
@AutoConfigureMockMvc(addFilters = false)
class TheaterControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TheaterService theaterService;

    @Test
    void 상영관_목록_조회() throws Exception {
        EnumMap<SeatType, Integer> seatCounts = new EnumMap<>(SeatType.class);

        seatCounts.put(SeatType.NORMAL, 10);
        seatCounts.put(SeatType.PREMIUM, 2);
        seatCounts.put(SeatType.ROOM, 1);

        TheaterListResponse response = new TheaterListResponse(
                List.of(new TheaterListResponse.TheaterInfo(
                        1L, "1관", TheaterType.FOUR_DX, 50, seatCounts
                ))
        );

        given(theaterService.getAllTheaterInfo()).willReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/theaters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theaterInfoList[0].theaterName").value("1관"))
                .andExpect(jsonPath("$.data.theaterInfoList[0].theaterType").value("4DX관"))
                .andExpect(jsonPath("$.data.theaterInfoList[0].seatCount.일반").value(10))
                .andExpect(jsonPath("$.data.theaterInfoList[0].seatCount.프리미엄").value(2))
                .andExpect(jsonPath("$.data.theaterInfoList[0].seatCount.방").value(1))
                .andDo(print());
    }
}