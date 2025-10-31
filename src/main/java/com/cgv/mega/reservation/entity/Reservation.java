package com.cgv.mega.reservation.entity;

import com.cgv.mega.screening.entity.ScreeningSeat;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "reservations",
        uniqueConstraints = @UniqueConstraint(name = "uq_reservations_screening_seat", columnNames = {"screening_seat_id"}),
        indexes = @Index(name = "idx_reservations_reservation_group", columnList = "reservation_group_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_reservation_group"))
    private ReservationGroup reservationGroup;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screening_seat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_screening_seat"))
    private ScreeningSeat screeningSeat;

    @Column(nullable = false)
    private int price;

    @Builder(access = AccessLevel.PRIVATE)
    private Reservation(ReservationGroup reservationGroup, ScreeningSeat screeningSeat, int price) {
        this.reservationGroup = reservationGroup;
        this.screeningSeat = screeningSeat;
        this.price = price;
    }

    public static Reservation createReservation(ReservationGroup reservationGroup, ScreeningSeat screeningSeat, int price) {
        return Reservation.builder()
                .reservationGroup(reservationGroup)
                .screeningSeat(screeningSeat)
                .price(price)
                .build();
    }
}