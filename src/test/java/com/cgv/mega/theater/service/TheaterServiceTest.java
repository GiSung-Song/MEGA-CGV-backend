package com.cgv.mega.theater.service;

import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.seat.repository.SeatQueryRepository;
import com.cgv.mega.theater.dto.TheaterListResponse;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.entity.TheaterFixture;
import com.cgv.mega.theater.enums.TheaterType;
import com.cgv.mega.theater.repository.TheaterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TheaterServiceTest {
    @Mock
    private SeatQueryRepository seatQueryRepository;

    @Mock
    private TheaterRepository theaterRepository;

    @InjectMocks
    private TheaterService theaterService;

    @Test
    void 상영관_목록_조회() {
        Theater theater1 = TheaterFixture.createTheater(1L, "1관", 50, TheaterType.IMAX);
        Theater theater2 = TheaterFixture.createTheater(2L, "2관", 20, TheaterType.FOUR_DX);

        Map<Long, Map<SeatType, Integer>> mock = Map.of(
                theater1.getId(), Map.of(
                        SeatType.NORMAL, 40,
                        SeatType.PREMIUM, 7,
                        SeatType.ROOM, 3
                ),
                theater2.getId(), Map.of(
                        SeatType.NORMAL, 15,
                        SeatType.PREMIUM, 5
                )
        );

        given(theaterRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))).willReturn(List.of(theater1, theater2));
        given(seatQueryRepository.getSeatCountGroupByTheater()).willReturn(mock);

        TheaterListResponse response = theaterService.getAllTheaterInfo();

        List<TheaterListResponse.TheaterInfo> theaterInfoList = response.theaterInfoList();
        assertThat(theaterInfoList).hasSize(2);

        TheaterListResponse.TheaterInfo theater1Seat = findById(theaterInfoList, theater1.getId());
        TheaterListResponse.TheaterInfo theater2Seat = findById(theaterInfoList, theater2.getId());

        assertThat(theater1Seat.theaterName()).isEqualTo("1관");
        assertThat(theater1Seat.totalSeat()).isEqualTo(50);

        assertThat(theater2Seat.theaterName()).isEqualTo("2관");
        assertThat(theater2Seat.totalSeat()).isEqualTo(20);

        assertThat(theater1Seat.seatCount())
                .containsEntry(SeatType.NORMAL, 40)
                .containsEntry(SeatType.PREMIUM, 7)
                .containsEntry(SeatType.ROOM, 3)
                .hasSize(3);

        assertThat(theater2Seat.seatCount())
                .containsEntry(SeatType.NORMAL, 15)
                .containsEntry(SeatType.PREMIUM, 5)
                .hasSize(2);
    }

    private TheaterListResponse.TheaterInfo findById(List<TheaterListResponse.TheaterInfo> infoList, Long id) {
        return infoList.stream()
                .filter(it -> it.theaterId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}