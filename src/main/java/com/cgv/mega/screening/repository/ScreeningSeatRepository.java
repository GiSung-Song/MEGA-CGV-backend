package com.cgv.mega.screening.repository;

import com.cgv.mega.screening.entity.ScreeningSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScreeningSeatRepository extends JpaRepository<ScreeningSeat, Long> {
    Optional<ScreeningSeat> findByScreeningIdAndSeatId(Long screeningId, Long seatId);
}