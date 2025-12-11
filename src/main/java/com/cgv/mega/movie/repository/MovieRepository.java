package com.cgv.mega.movie.repository;

import com.cgv.mega.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query("""
        select distinct m
          from Movie m
          left join fetch m.movieGenres mg
          left join fetch mg.genre g
          left join fetch m.movieTypes mt
         where m.id = :movieId
    """)
    Optional<Movie> findByIdWithGenresAndTypes(@Param("movieId") Long moveId);
}