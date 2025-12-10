package com.cgv.mega.seat.repository;

import com.cgv.mega.common.config.JpaConfig;
import com.cgv.mega.common.config.QueryDslConfig;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.seat.enums.SeatType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, SeatQueryRepository.class, QueryDslConfig.class})
@ActiveProfiles("test")
class SeatQueryRepositoryTest {

    @Autowired
    private SeatQueryRepository seatQueryRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.registerMySQL(registry);
    }

    @Test
    void 상영관별_좌석타입별_개수() {
        Map<Long, Map<SeatType, Integer>> result = seatQueryRepository.getSeatCountGroupByTheater();

        Map<SeatType, Integer> theater1Seats = result.get(1L);
        Map<SeatType, Integer> theater3Seats = result.get(3L);
        Map<SeatType, Integer> theater4Seats = result.get(4L);

        assertThat(theater1Seats)
                .containsEntry(SeatType.NORMAL, 40)
                .containsEntry(SeatType.PREMIUM, 7)
                .containsEntry(SeatType.ROOM, 3);

        assertThat(theater3Seats)
                .containsEntry(SeatType.NORMAL, 20)
                .containsEntry(SeatType.PREMIUM, 5)
                .containsEntry(SeatType.ROOM, 5);

        assertThat(theater4Seats)
                .containsEntry(SeatType.NORMAL, 10)
                .containsEntry(SeatType.PREMIUM, 5)
                .containsEntry(SeatType.ROOM, 5);
    }
}