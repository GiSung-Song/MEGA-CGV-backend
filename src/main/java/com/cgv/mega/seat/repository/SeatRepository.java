package com.cgv.mega.seat.repository;

import com.cgv.mega.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    Set<Seat> findByTheaterId(Long theaterId);
    Optional<Seat> findByTheaterIdAndRowLabelAndColNumber(Long theaterId, String rowLabel, int colNumber);
}
