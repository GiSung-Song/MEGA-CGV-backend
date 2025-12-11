package com.cgv.mega.theater.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.theater.enums.TheaterType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(
        name = "theaters",
        uniqueConstraints = @UniqueConstraint(name = "uq_theaters_name", columnNames = {"name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class Theater extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, name = "total_seat")
    private int totalSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TheaterType type;
}