package com.cgv.mega.reservation.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.ReservationStatus;
import com.cgv.mega.screening.entity.ScreeningSeat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "reservation_groups",
        indexes = @Index(name = "idx_reservation_groups_user", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @OneToMany(mappedBy = "reservationGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private ReservationGroup(Long userId) {
        this.userId = userId;
        this.status = ReservationStatus.PENDING;
    }

    public static ReservationGroup createReservationGroup(Long userId) {
        return ReservationGroup.builder()
                .userId(userId)
                .build();
    }

    public void addReservation(ScreeningSeat screeningSeat, int price) {
        Reservation reservation = Reservation.createReservation(this, screeningSeat, price);

        boolean exists = this.reservations.stream()
                .anyMatch(r -> r.getScreeningSeat().equals(screeningSeat));

        if (!exists) {
            this.reservations.add(reservation);
            this.totalPrice += price;
        }
    }

    public void updateReservationStatus(ReservationStatus status) {
        this.status = status;
    }
}
