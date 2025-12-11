package com.cgv.mega.screening.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.screening.enums.ScreeningStatus;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.theater.entity.Theater;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "screenings",
        uniqueConstraints = @UniqueConstraint(name = "uq_screenings_sequence", columnNames = {"movie_id", "sequence"}),
        indexes = @Index(name = "idx_screenings_status_start", columnList = "status, start_time")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreeningStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "movie_type")
    private MovieType movieType;

    @OneToMany(mappedBy = "screening", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ScreeningSeat> screeningSeats = new HashSet<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Screening(Movie movie, Theater theater, LocalDateTime startTime, LocalDateTime endTime, int sequence, MovieType movieType) {
        this.movie = movie;
        this.theater = theater;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sequence = sequence;
        this.status = ScreeningStatus.SCHEDULED;
        this.movieType = movieType;
    }

    public static Screening createScreening(Movie movie, Theater theater, LocalDateTime startTime,
                                            LocalDateTime endTime, int sequence, MovieType movieType) {
        return Screening.builder()
                .movie(movie)
                .theater(theater)
                .startTime(startTime)
                .endTime(endTime)
                .sequence(sequence)
                .movieType(movieType)
                .build();
    }

    public void initializeSeats(Set<Seat> seats, int basePrice) {
        double theaterPrice = this.theater.getType().getMultiplier();
        double movieTypePrice = this.movieType.getMultiplier();

        for (Seat seat : seats) {
            double seatTypePrice = seat.getType().getMultiplier();

            int price = (int) (basePrice * theaterPrice * movieTypePrice * seatTypePrice);

            this.screeningSeats.add(ScreeningSeat.createScreeningSeat(this, seat, price));
        }
    }

    public void cancelScreening() {
        this.status = ScreeningStatus.CANCELED;
    }

    public void markEnded() {
        this.status = ScreeningStatus.ENDED;
    }

    public boolean isEnded() {
        return this.status == ScreeningStatus.ENDED
                || LocalDateTime.now().isAfter(this.endTime);
    }

    public void validateReservable(LocalDateTime now) {
        // 예약 상태 확인
        if (this.getStatus() != ScreeningStatus.SCHEDULED) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_AVAILABLE_STATUS);
        }

        // 상영 시작 10분 전까지 예약 가능
        if (!now.isBefore(this.getStartTime().minusMinutes(10))) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_AVAILABLE_TIME);
        }
    }
}