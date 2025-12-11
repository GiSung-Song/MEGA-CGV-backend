package com.cgv.mega.movie.entity;

import com.cgv.mega.movie.enums.MovieType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movie_types")
@IdClass(MovieTypeMappingId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovieTypeMapping {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_movie_types_movie"))
    @EqualsAndHashCode.Include
    private Movie movie;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    @EqualsAndHashCode.Include
    private MovieType type;

    @Builder(access = AccessLevel.PRIVATE)
    private MovieTypeMapping(Movie movie, MovieType type) {
        this.movie = movie;
        this.type = type;
    }

    public static MovieTypeMapping createMovieTypeMapping(Movie movie, MovieType type) {
        return MovieTypeMapping.builder()
                .movie(movie)
                .type(type)
                .build();
    }
}
