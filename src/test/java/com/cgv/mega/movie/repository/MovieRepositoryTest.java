package com.cgv.mega.movie.repository;

import com.cgv.mega.common.config.JpaConfig;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.entity.Movie;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@ActiveProfiles("test")
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @PersistenceContext
    private EntityManager em;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.registerMySQL(registry);
    }

    @Test
    void 장르_및_타입_포함_영화_조회() {
        Movie movie = Movie.createMovie("혹성탈출", 210, "혹성 탈출 설명", "poster.png");

        Genre action = genreRepository.findById(1L)
                .orElseThrow();

        Genre drama = genreRepository.findById(2L)
                .orElseThrow();

        movie.addGenre(action);
        movie.addGenre(drama);

        movie.addType(MovieType.TWO_D);
        movie.addType(MovieType.THREE_D);

        movieRepository.save(movie);

        em.flush();
        em.clear();

        Movie savedMovie = movieRepository.findByIdWithGenresAndTypes(movie.getId())
                .orElseThrow();

        assertThat(savedMovie.getMovieGenres())
                .extracting(mg -> mg.getGenre().getName())
                .containsExactlyInAnyOrder("ACTION", "DRAMA");

        assertThat(savedMovie.getMovieTypes())
                .extracting(mt -> mt.getType().getValue())
                .containsExactlyInAnyOrder("2D", "3D");
    }
}