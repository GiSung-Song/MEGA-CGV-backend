package com.cgv.mega.reservation.repository;

import com.cgv.mega.reservation.entity.ReservationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationGroupRepository extends JpaRepository<ReservationGroup, Long> {

    @Query("""
            select distinct rg
              from ReservationGroup rg
              join fetch rg.reservations r
              join fetch r.screeningSeat sc
             where rg.id = :reservationGroupId
               and rg.userId = :userId
            """)
    Optional<ReservationGroup> findByIdAndUserId(
            @Param("reservationGroupId") Long reservationGroupId,
            @Param("userId") Long userId);

    @Query("""
                select distinct rg
                  from ReservationGroup rg
                  join fetch rg.reservations r
                  join fetch r.screeningSeat ss
                  join fetch ss.screening sc
                 where sc.id = :screeningId
            """)
    List<ReservationGroup> findAllByScreeningId(Long screeningId);
}