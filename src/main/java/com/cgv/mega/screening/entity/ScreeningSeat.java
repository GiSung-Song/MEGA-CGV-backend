package com.cgv.mega.screening.entity;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.seat.entity.Seat;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "screening_seats",
        uniqueConstraints = @UniqueConstraint(name = "uq_screening_seats_screen_seat", columnNames = {"screening_id", "seat_id"}),
        indexes = @Index(name = "idx_screening_seats_screen", columnList = "screening_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ScreeningSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screening_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_screening_seats_screening"))
    @EqualsAndHashCode.Include
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_screening_seats_seat"))
    @EqualsAndHashCode.Include
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ScreeningSeatStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private ScreeningSeat(Screening screening, Seat seat) {
        this.screening = screening;
        this.seat = seat;
        this.status = ScreeningSeatStatus.AVAILABLE;
    }

    public static ScreeningSeat createScreeningSeat(Screening screening, Seat seat) {
        return ScreeningSeat.builder()
                .screening(screening)
                .seat(seat)
                .build();
    }

    public void reserveScreeningSeat() {
        if (this.status != ScreeningSeatStatus.AVAILABLE) {
            throw new CustomException(ErrorCode.SCREENING_SEAT_NOT_AVAILABLE);
        }

        this.status = ScreeningSeatStatus.RESERVED;
    }

    public void cancelScreeningSeat() {
        if (this.status != ScreeningSeatStatus.RESERVED) {
            throw new CustomException(ErrorCode.SCREENING_SEAT_NOT_RESERVED);
        }

        this.status = ScreeningSeatStatus.AVAILABLE;
    }
}
