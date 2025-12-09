package com.cgv.mega.screening.repository;

import com.cgv.mega.common.config.JpaConfig;
import com.cgv.mega.common.config.QueryDslConfig;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.screening.dto.MovieScreeningResponse;
import com.cgv.mega.screening.dto.ScreeningDateMovieResponse;
import com.cgv.mega.screening.dto.ScreeningSeatDto;
import com.cgv.mega.screening.dto.ScreeningTimeDto;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, ScreeningQueryRepository.class, QueryDslConfig.class})
@ActiveProfiles("test")
class ScreeningQueryRepositoryTest {

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @Autowired
    private ScreeningQueryRepository screeningQueryRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.registerMySQL(registry);
    }

    private Theater theater;
    private Movie movie;

    private Screening screening1;
    private Screening screening2;
    private Screening screening3;

    @BeforeEach
    void setUp() {
        movie = Movie.createMovie("인터스텔라", 150, "인터스텔라 설명", "인터스텔라.png");

        Genre action = genreRepository.findById(1L)
                .orElseThrow();

        Genre drama = genreRepository.findById(2L)
                .orElseThrow();

        movie.addGenre(action);
        movie.addGenre(drama);
        movie.addType(MovieType.TWO_D);
        movie.addType(MovieType.THREE_D);

        movieRepository.save(movie);

        theater = theaterRepository.findById(1L)
                .orElseThrow();

        Set<Seat> seats = seatRepository.findByTheaterId(theater.getId());

        LocalDateTime startTime1 = LocalDateTime.of(2026, 11, 11, 6, 00);
        LocalDateTime endTime1 = startTime1.plusMinutes(movie.getDuration()).plusMinutes(15);

        screening1 = Screening.createScreening(movie, theater, startTime1, endTime1, 1, MovieType.TWO_D);
        screening1.initializeSeats(seats, 1000);

        LocalDateTime startTime2 = startTime1.plusHours(3);
        LocalDateTime endTime2 = startTime2.plusMinutes(movie.getDuration()).plusMinutes(15);

        screening2 = Screening.createScreening(movie, theater, startTime2, endTime2, 2, MovieType.TWO_D);
        screening2.initializeSeats(seats, 1000);

        LocalDateTime startTime3 = startTime1.plusHours(10);
        LocalDateTime endTime3 = startTime3.plusMinutes(movie.getDuration()).plusMinutes(15);

        screening3 = Screening.createScreening(movie, theater, startTime3, endTime3, 3, MovieType.TWO_D);
        screening3.initializeSeats(seats, 1000);

        screeningRepository.saveAll(List.of(screening1, screening2, screening3));
    }

    @Test
    void 모든_상영_예정_시간_조회() {
        List<ScreeningTimeDto> reservedScreening =
                screeningQueryRepository.getReservedScreening(theater.getId(), LocalDate.of(2026, 11, 11));

        assertThat(reservedScreening.size()).isEqualTo(3);
        assertThat(reservedScreening.get(0).startTime()).isEqualTo(LocalDateTime.of(2026, 11, 11, 6, 00));
        assertThat(reservedScreening.get(2).endTime()).isEqualTo(LocalDateTime.of(2026, 11, 11, 18, 45));
    }

    @Test
    void 시간이_겹치는지_체크() {
        LocalDateTime trueStart = LocalDateTime.of(2026, 11, 11, 7, 00);
        LocalDateTime trueEnd = LocalDateTime.of(2026, 11, 11, 9, 00);

        LocalDateTime falseStart = LocalDateTime.of(2026, 11, 11, 12, 00);
        LocalDateTime falseEnd = LocalDateTime.of(2026, 11, 11, 15, 00);

        boolean isTrue = screeningQueryRepository.existsOverlap(theater.getId(), trueStart, trueEnd);
        boolean isFalse = screeningQueryRepository.existsOverlap(theater.getId(), falseStart, falseEnd);

        assertThat(isTrue).isTrue();
        assertThat(isFalse).isFalse();
    }

    @Test
    void 영화_상영_회차_조회() {
        int existsMovieSequence = screeningQueryRepository.getMovieSequence(movie.getId());
        int newMovieSequence = screeningQueryRepository.getMovieSequence(123L);

        assertThat(existsMovieSequence).isEqualTo(4);
        assertThat(newMovieSequence).isEqualTo(1);
    }

    @Test
    void 상영중인_영화_목록_조회() {
        LocalDate existsDate = LocalDate.of(2026, 11, 11);
        LocalDate noneDate = LocalDate.of(2026, 11, 12);

        List<ScreeningDateMovieResponse.MovieInfo> screeningMovieList = screeningQueryRepository.getScreeningMovieList(existsDate);
        List<ScreeningDateMovieResponse.MovieInfo> emptyList = screeningQueryRepository.getScreeningMovieList(noneDate);

        assertThat(screeningMovieList.size()).isEqualTo(1);
        assertThat(screeningMovieList.get(0).title()).isEqualTo(movie.getTitle());
        assertThat(emptyList).isEmpty();
    }

    @Test
    void 특정_영화_상영_일정_조회() {
        List<MovieScreeningResponse.MovieScreeningInfo> movieScreeningList = screeningQueryRepository.getMovieScreeningList(
                movie.getId(), LocalDate.of(2026, 11, 11)
        );

        LocalDateTime startTime1 = LocalDateTime.of(2026, 11, 11, 6, 00);
        LocalDateTime startTime2 = startTime1.plusHours(3);
        LocalDateTime startTime3 = startTime1.plusHours(10);

        assertThat(movieScreeningList).hasSize(3)
                .extracting(ms -> ms.startTime())
                .containsExactly(startTime1, startTime2, startTime3);
    }

    @Test
    void 상영회차_좌석_현황_조회() {
        Seat seat = seatRepository.findByTheaterIdAndRowLabelAndColNumber(
                        theater.getId(), "A", 1)
                .orElseThrow();

        ScreeningSeat reservedSeat = screeningSeatRepository.findByScreeningIdAndSeatId(screening1.getId(), seat.getId())
                .orElseThrow();

        reservedSeat.reserveScreeningSeat();

        List<ScreeningSeatDto> screeningSeat = screeningQueryRepository.getScreeningSeat(screening1.getId());


        assertThat(screeningSeat.get(0).rowLabel()).isEqualTo("A");
        assertThat(screeningSeat.get(0).colNumber()).isEqualTo(1);
        assertThat(screeningSeat.get(0).status()).isEqualTo(ScreeningSeatStatus.RESERVED);

        assertThat(screeningSeat.get(1).rowLabel()).isEqualTo("A");
        assertThat(screeningSeat.get(1).colNumber()).isEqualTo(2);
        assertThat(screeningSeat.get(1).status()).isEqualTo(ScreeningSeatStatus.AVAILABLE);
    }
}