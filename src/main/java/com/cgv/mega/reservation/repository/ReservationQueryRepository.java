package com.cgv.mega.reservation.repository;

import com.cgv.mega.reservation.dto.ReservationDetailDto;
import com.cgv.mega.reservation.dto.ReservationListDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.cgv.mega.movie.entity.QMovie.movie;
import static com.cgv.mega.payment.entity.QPayment.payment;
import static com.cgv.mega.reservation.entity.QReservation.reservation;
import static com.cgv.mega.reservation.entity.QReservationGroup.reservationGroup;
import static com.cgv.mega.screening.entity.QScreening.screening;
import static com.cgv.mega.screening.entity.QScreeningSeat.screeningSeat;
import static com.cgv.mega.seat.entity.QSeat.seat;
import static com.cgv.mega.theater.entity.QTheater.theater;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@Repository
@RequiredArgsConstructor
public class ReservationQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ReservationDetailDto getReservationDetail(Long userId, Long reservationGroupId) {
        List<ReservationDetailDto> results = jpaQueryFactory
                .from(reservationGroup)
                .join(payment).on(payment.reservationGroup.eq(reservationGroup))
                .join(reservationGroup.reservations, reservation)
                .join(reservation.screeningSeat, screeningSeat)
                .join(screeningSeat.seat, seat)
                .join(screeningSeat.screening, screening)
                .join(screening.movie, movie)
                .join(screening.theater, theater)
                .where(
                        reservationGroup.id.eq(reservationGroupId),
                        reservationGroup.userId.eq(userId)
                )
                .transform(
                        groupBy(reservationGroup.id).list(
                                Projections.constructor(
                                        ReservationDetailDto.class,

                                        movie.id,
                                        movie.title,
                                        screening.movieType,
                                        movie.posterUrl,
                                        movie.duration,

                                        screening.id,
                                        screening.startTime,
                                        screening.endTime,

                                        theater.id,
                                        theater.name,
                                        theater.type,

                                        list(Projections.constructor(
                                                ReservationDetailDto.SeatInfo.class,
                                                seat.rowLabel,
                                                seat.colNumber,
                                                seat.type
                                        )),

                                        reservationGroup.id,
                                        reservationGroup.status,
                                        reservationGroup.createdAt,
                                        reservationGroup.updatedAt,

                                        payment.status,
                                        payment.payMethod,
                                        payment.paidAmount,
                                        payment.refundAmount,
                                        payment.merchantUid,
                                        payment.paymentId,

                                        payment.buyerName,
                                        payment.buyerPhoneNumber,
                                        payment.buyerEmail
                                )
                        )
                );

        return results.isEmpty() ? null : results.get(0);
    }

    public Page<ReservationListDto> getReservationList(Long userId, Pageable pageable) {
        Long total = jpaQueryFactory
                .select(reservationGroup.id.countDistinct())
                .from(reservationGroup)
                .where(reservationGroup.userId.eq(userId))
                .fetchOne();

        List<ReservationListDto> transform = jpaQueryFactory
                .from(reservationGroup)
                .join(reservationGroup.reservations, reservation)
                .join(reservation.screeningSeat, screeningSeat)
                .join(screeningSeat.seat, seat)
                .join(screeningSeat.screening, screening)
                .join(screening.theater, theater)
                .join(screening.movie, movie)
                .where(
                        reservationGroup.userId.eq(userId)
                )
                .orderBy(screening.startTime.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .transform(
                        groupBy(reservationGroup.id).list(
                                Projections.constructor(
                                        ReservationListDto.class,
                                        reservationGroup.id,
                                        movie.title,
                                        screening.movieType,
                                        screening.startTime,
                                        theater.name,
                                        theater.type,
                                        list(
                                                Projections.constructor(
                                                        ReservationListDto.SeatDto.class,
                                                        seat.rowLabel,
                                                        seat.colNumber,
                                                        seat.type
                                                )
                                        ),
                                        reservationGroup.status,
                                        reservationGroup.totalPrice,
                                        movie.posterUrl,
                                        reservationGroup.updatedAt
                                )
                        )
                );

        return new PageImpl<>(transform, pageable, total);
    }
}
