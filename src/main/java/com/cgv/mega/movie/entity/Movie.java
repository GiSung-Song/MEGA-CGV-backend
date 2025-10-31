package com.cgv.mega.movie.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.genre.entity.Genre;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "movies",
        indexes = @Index(name = "idx_movies_title", columnList = "title")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieGenre> movieGenres = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieTypeMapping> movieTypes = new ArrayList<>();

    @Builder
    public Movie(String title, int duration, String description, String posterUrl) {
        this.title = title;
        this.duration = duration;
        this.description = description;
        this.posterUrl = posterUrl;
    }

    public void addGenre(Genre genre) {
        MovieGenre movieGenre = MovieGenre.createMovieGenre(this, genre);

        boolean exists = this.movieGenres.stream()
                .anyMatch(mg -> mg.getGenre().equals(genre));

        if (!exists) this.movieGenres.add(movieGenre);
    }

    public void addType(MovieType movieType) {
        MovieTypeMapping movieTypeMapping = MovieTypeMapping.createMovieTypeMapping(this, movieType);

        boolean exists = this.movieTypes.stream()
                .anyMatch(mt -> mt.getType().equals(movieType));

        if (!exists) this.movieTypes.add(movieTypeMapping);
    }
}