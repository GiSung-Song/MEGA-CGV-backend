package com.cgv.mega.screening.repository;

import com.cgv.mega.screening.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    boolean existsByMovieIdAndEndTimeAfter(Long movieId, LocalDateTime endTime);
    boolean existsByMovieId(Long moveId);
}
