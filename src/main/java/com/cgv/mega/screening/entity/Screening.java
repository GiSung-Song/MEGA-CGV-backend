package com.cgv.mega.screening.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.theater.entity.Theater;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "screenings",
        uniqueConstraints = @UniqueConstraint(name = "uq_screenings_sequence", columnNames = {"movie_id", "sequence"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Screening extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_screenings_movie"))
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theater_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_screenings_theater"))
    private Theater theater;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private int sequence;

    @OneToMany(mappedBy = "screening", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScreeningSeat> screeningSeats = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Screening(Movie movie, Theater theater, LocalDateTime startTime, LocalDateTime endTime, int sequence) {
        this.movie = movie;
        this.theater = theater;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sequence = sequence;
    }

    public static Screening createScreening(Movie movie, Theater theater, LocalDateTime startTime, LocalDateTime endTime, int sequence) {
        return Screening.builder()
                .movie(movie)
                .theater(theater)
                .startTime(startTime)
                .endTime(endTime)
                .sequence(sequence)
                .build();
    }

    public void initializeSeats(List<Seat> seats) {
        for (Seat seat : seats) {
            boolean exists = this.screeningSeats.stream()
                    .anyMatch(ss -> ss.getSeat().equals(seat));

            if (!exists) {
                this.screeningSeats.add(ScreeningSeat.createScreeningSeat(this, seat));
            }
        }
    }

    public void removeSeat(ScreeningSeat screeningSeat) {
        screeningSeats.remove(screeningSeat);
    }
}
