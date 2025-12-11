package com.cgv.mega.screening.repository;

import com.cgv.mega.common.config.JpaConfig;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.enums.ScreeningStatus;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public class ScreeningRepositoryTest {
    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private GenreRepository genreRepository;

    @PersistenceContext
    private EntityManager em;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.registerMySQL(registry);
    }

    private Theater theater;
    private Movie movie;

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
    }

    @Test
    void 상영_종료_일괄_변경() {
        Screening pastScreen = Screening.createScreening(movie, theater,
                LocalDateTime.of(2000, 10, 10, 10, 0),
                LocalDateTime.of(2000, 10, 10, 13, 0),
                1, MovieType.TWO_D
        );

        Screening pastScreen2 = Screening.createScreening(movie, theater,
                LocalDateTime.of(2010, 10, 10, 10, 0),
                LocalDateTime.of(2010, 10, 10, 13, 0),
                2, MovieType.TWO_D
        );

        Screening futureScreen = Screening.createScreening(movie, theater,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                LocalDateTime.of(2026, 10, 10, 13, 0),
                3, MovieType.TWO_D
        );

        screeningRepository.saveAll(List.of(pastScreen, pastScreen2, futureScreen));

        em.flush();
        em.clear();

        screeningRepository.updateStatusToEnded(
                LocalDateTime.now(), ScreeningStatus.ENDED, ScreeningStatus.SCHEDULED
        );

        em.flush();
        em.clear();

        Screening pastFind = screeningRepository.findById(pastScreen.getId())
                .orElseThrow();

        Screening pastFind2 = screeningRepository.findById(pastScreen2.getId())
                .orElseThrow();

        Screening futureFind = screeningRepository.findById(futureScreen.getId())
                .orElseThrow();

        assertThat(pastFind.getStatus()).isEqualTo(ScreeningStatus.ENDED);
        assertThat(pastFind2.getStatus()).isEqualTo(ScreeningStatus.ENDED);
        assertThat(futureFind.getStatus()).isEqualTo(ScreeningStatus.SCHEDULED);
    }
}
