package com.cgv.mega.reservation.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.ReservationStatus;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_groups_user"))
    private User user;

    @Column(nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @OneToMany(mappedBy = "reservationGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private ReservationGroup(User user) {
        this.user = user;
    }

    public static ReservationGroup createReservationGroup(User user) {
        return ReservationGroup.builder()
                .user(user)
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
