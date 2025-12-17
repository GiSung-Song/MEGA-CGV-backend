package com.cgv.mega.movie.repository;

import com.cgv.mega.movie.dto.MovieListResponse;
import com.cgv.mega.movie.dto.MovieListResponseBuilder;
import com.cgv.mega.movie.enums.MovieStatus;
import com.cgv.mega.movie.enums.MovieType;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cgv.mega.genre.entity.QGenre.genre;
import static com.cgv.mega.movie.entity.QMovie.movie;
import static com.cgv.mega.movie.entity.QMovieGenre.movieGenre;
import static com.cgv.mega.movie.entity.QMovieTypeMapping.movieTypeMapping;

@Repository
@RequiredArgsConstructor
public class MovieQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<MovieListResponse> getAllMovieList(Pageable pageable) {
        Long total = jpaQueryFactory
                .select(movie.id.countDistinct())
                .from(movie)
                .where(movie.status.eq(MovieStatus.ACTIVE))
                .fetchOne();

        List<Long> movieIds = jpaQueryFactory
                .select(movie.id)
                .from(movie)
                .where(movie.status.eq(MovieStatus.ACTIVE))
                .orderBy(movie.title.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (movieIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<Tuple> rows = jpaQueryFactory
                .select(
                        movie.id,
                        movie.title,
                        movie.posterUrl,
                        genre.name,
                        movieTypeMapping.type
                )
                .from(movie)
                .leftJoin(movie.movieGenres, movieGenre)
                .leftJoin(movieGenre.genre, genre)
                .leftJoin(movie.movieTypes, movieTypeMapping)
                .where(movie.id.in(movieIds))
                .fetch();

        Map<Long, MovieListResponseBuilder> map = new LinkedHashMap<>();

        for (Tuple row : rows) {
            Long movieId = row.get(movie.id);

            map.computeIfAbsent(movieId, id ->
                    new MovieListResponseBuilder(
                            id,
                            row.get(movie.title),
                            new HashSet<>(),
                            new HashSet<>(),
                            row.get(movie.posterUrl)
                    )
            );

            String genreName = row.get(genre.name);
            if (genreName != null) {
                map.get(movieId).genres().add(genreName);
            }

            MovieType type = row.get(movieTypeMapping.type);
            if (type != null) {
                map.get(movieId).types().add(type.getValue());
            }
        }

        List<MovieListResponse> content = map.values()
                .stream()
                .map(MovieListResponseBuilder::build)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}