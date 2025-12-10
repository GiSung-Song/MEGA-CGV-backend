package com.cgv.mega.seat.repository;

import com.cgv.mega.seat.enums.SeatType;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cgv.mega.seat.entity.QSeat.seat;

@Repository
@RequiredArgsConstructor
public class SeatQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Map<Long, Map<SeatType, Integer>> getSeatCountGroupByTheater() {
        List<Tuple> rows = jpaQueryFactory
                .select(
                        seat.theater.id,
                        seat.type,
                        seat.id.count()
                )
                .from(seat)
                .groupBy(seat.theater.id, seat.type)
                .fetch();

        Map<Long, Map<SeatType, Integer>> result = new HashMap<>();

        for (Tuple row : rows) {
            Long theaterId = row.get(seat.theater.id);
            SeatType seatType = row.get(seat.type);
            Integer count = row.get(seat.id.count()).intValue();

            result
                    .computeIfAbsent(theaterId, id -> new EnumMap<SeatType, Integer>(SeatType.class))
                    .put(seatType, count);
        }

        return result;
    }
}