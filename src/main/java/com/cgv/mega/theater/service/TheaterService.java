package com.cgv.mega.theater.service;

import com.cgv.mega.seat.enums.SeatType;
import com.cgv.mega.seat.repository.SeatQueryRepository;
import com.cgv.mega.theater.dto.TheaterListResponse;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final SeatQueryRepository seatQueryRepository;

    @Cacheable(cacheNames = "theaters", cacheManager = "caffeineCacheManager")
    @Transactional(readOnly = true)
    public TheaterListResponse getAllTheaterInfo() {
        List<Theater> theaters = theaterRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        Map<Long, Map<SeatType, Integer>> seatCountMap = seatQueryRepository.getSeatCountGroupByTheater();

        List<TheaterListResponse.TheaterInfo> theaterInfoList = theaters.stream()
                .map(th -> new TheaterListResponse.TheaterInfo(
                        th.getId(),
                        th.getName(),
                        th.getType(),
                        th.getTotalSeat(),
                        seatCountMap.getOrDefault(th.getId(), Map.of())
                ))
                .toList();

        return new TheaterListResponse(theaterInfoList);
    }
}
