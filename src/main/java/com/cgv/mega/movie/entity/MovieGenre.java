package com.cgv.mega.movie.entity;

import com.cgv.mega.genre.entity.Genre;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movie_genres")
@IdClass(MovieGenreId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovieGenre {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_movie_genres_movie"))
    @EqualsAndHashCode.Include
    private Movie movie;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_movie_genres_genre"))
    @EqualsAndHashCode.Include
    private Genre genre;

    @Builder(access = AccessLevel.PRIVATE)
    private MovieGenre(Movie movie, Genre genre) {
        this.movie = movie;
        this.genre = genre;
    }

    public static MovieGenre createMovieGenre(Movie movie, Genre genre) {
        return MovieGenre.builder()
                .movie(movie)
                .genre(genre)
                .build();
    }
}