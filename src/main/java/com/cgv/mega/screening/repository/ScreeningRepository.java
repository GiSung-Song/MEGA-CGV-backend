package com.cgv.mega.screening.repository;

import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.enums.ScreeningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    boolean existsByMovieId(Long moveId);

    @Query("""
            select distinct sc.startTime
              from ReservationGroup rg
              join rg.reservations r
              join r.screeningSeat ss
              join ss.screening sc
             where rg.id = :reservationGroupId
               and rg.userId = :userId
            """)
    LocalDateTime findScreeningStartTime(@Param("reservationGroupId") Long reservationGroupId,
                                         @Param("userId") Long userId);

    @Modifying
    @Query("""
                update Screening s
                   set s.status = :ended
                 where s.status = :scheduled
                   and s.startTime <= :now
            """)
    int updateStatusToEnded(
            @Param("now") LocalDateTime now,
            @Param("ended") ScreeningStatus ended,
            @Param("scheduled") ScreeningStatus scheduled
    );
}