package com.cgv.mega.seat.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.SeatType;
import com.cgv.mega.theater.entity.Theater;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uq_seats_screen_row_col", columnNames = {"theater_id", "row_label", "col_number"}),
        indexes = @Index(name = "idx_seats_theater", columnList = "theater_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Seat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theater_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_seats_theater"))
    private Theater theater;

    @Column(name = "row_label", nullable = false, length = 1)
    private String rowLabel;

    @Column(name = "col_number", nullable = false)
    private int colNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatType type;
}
