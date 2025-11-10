package com.cgv.mega.movie.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.movie.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "movies",
        indexes = @Index(name = "idx_movies_title", columnList = "title")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Movie extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private int duration;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500, name = "poster_url")
    private String posterUrl;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MovieGenre> movieGenres = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MovieTypeMapping> movieTypes = new HashSet<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Movie(String title, int duration, String description, String posterUrl) {
        this.title = title;
        this.duration = duration;
        this.description = description;
        this.posterUrl = posterUrl;
        this.status = MovieStatus.ACTIVE;
    }

    public static Movie createMovie(String title, int duration, String description, String posterUrl) {
        return Movie.builder()
                .title(title)
                .duration(duration)
                .description(description)
                .posterUrl(posterUrl)
                .build();
    }

    public void addGenre(Genre genre) {
        MovieGenre movieGenre = MovieGenre.createMovieGenre(this, genre);
        this.movieGenres.add(movieGenre);
    }

    public void addType(MovieType movieType) {
        MovieTypeMapping movieTypeMapping = MovieTypeMapping.createMovieTypeMapping(this, movieType);
        this.movieTypes.add(movieTypeMapping);
    }

    public void deactivate() {
        this.status = MovieStatus.INACTIVE;
    }
}