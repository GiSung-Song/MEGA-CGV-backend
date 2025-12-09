package com.cgv.mega.screening.repository;

import com.cgv.mega.screening.entity.ScreeningSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ScreeningSeatRepository extends JpaRepository<ScreeningSeat, Long> {
    Optional<ScreeningSeat> findByScreeningIdAndSeatId(Long screeningId, Long seatId);

    List<ScreeningSeat> findByIdInAndScreeningId(
            @Param("screeningSeatIds") Set<Long> screeningSeatIds,
            @Param("screeningId") Long screeningId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                    SELECT s
                      FROM ScreeningSeat s
                     WHERE s.screening.id = :screeningId
                       AND s.id IN :screeningSeatIds
            """)
    List<ScreeningSeat> findByIdInAndScreeningIdForUpdate(
            @Param("screeningSeatIds") Set<Long> screeningSeatIds,
            @Param("screeningId") Long screeningId
    );
}