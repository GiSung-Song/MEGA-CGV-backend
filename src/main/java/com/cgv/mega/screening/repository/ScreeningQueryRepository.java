package com.cgv.mega.screening.repository;

import com.cgv.mega.screening.dto.MovieScreeningResponse;
import com.cgv.mega.screening.dto.ScreeningDateMovieResponse;
import com.cgv.mega.screening.dto.ScreeningTimeDto;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.enums.ScreeningStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.cgv.mega.movie.entity.QMovie.movie;
import static com.cgv.mega.screening.entity.QScreening.screening;
import static com.cgv.mega.screening.entity.QScreeningSeat.screeningSeat;
import static com.cgv.mega.theater.entity.QTheater.theater;

@Repository
@RequiredArgsConstructor
public class ScreeningQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<ScreeningTimeDto> getReservedScreening(Long theaterId, LocalDate date) {
        return jpaQueryFactory
                .select(Projections.constructor(ScreeningTimeDto.class,
                        screening.startTime,
                        screening.endTime))
                .from(screening)
                .where(
                        screening.theater.id.eq(theaterId),
                        screening.status.eq(ScreeningStatus.SCHEDULED),
                        withInTime(date)
                )
                .orderBy(screening.startTime.asc())
                .fetch();
    }

    public boolean existsOverlap(Long theaterId, LocalDateTime startTime, LocalDateTime endTime) {
        return jpaQueryFactory
                .selectOne()
                .from(screening)
                .where(
                        screening.status.eq(ScreeningStatus.SCHEDULED),
                        screening.theater.id.eq(theaterId),
                        screening.startTime.lt(endTime),
                        screening.endTime.gt(startTime)
                )
                .fetchFirst() != null;
    }

    public int getMovieSequence(Long movieId) {
        Integer maxSequence = jpaQueryFactory
                .select(screening.sequence.max())
                .from(screening)
                .where(
                        screening.status.ne(ScreeningStatus.CANCELED),
                        screening.movie.id.eq(movieId)
                )
                .fetchOne();

        return maxSequence == null ? 1 : maxSequence + 1;
    }

    public List<ScreeningDateMovieResponse.MovieInfo> getScreeningMovieList(LocalDate date) {
        return jpaQueryFactory
                .selectDistinct(Projections.constructor(ScreeningDateMovieResponse.MovieInfo.class,
                        movie.id,
                        movie.title,
                        movie.posterUrl
                ))
                .from(screening)
                .join(screening.movie, movie)
                .where(
                        withInTime(date),
                        screening.status.eq(ScreeningStatus.SCHEDULED)
                )
                .fetch();
    }

    public List<MovieScreeningResponse.MovieScreeningInfo> getMovieScreeningList(Long movieId, LocalDate date) {
        BooleanExpression dateCondition = (date != null)
                ? withInTime(date)
                : null;

        List<MovieScreeningResponse.MovieScreeningInfo> raw = jpaQueryFactory
                .select(Projections.constructor(MovieScreeningResponse.MovieScreeningInfo.class,
                        screening.id,
                        theater.id,
                        theater.name,
                        JPAExpressions.select(screeningSeat.id.count())
                                .from(screeningSeat)
                                .where(screeningSeat.screening.id.eq(screening.id),
                                        screeningSeat.status.eq(ScreeningSeatStatus.AVAILABLE)),
                        screening.startTime,
                        screening.endTime,
                        screening.sequence
                ))
                .from(screening)
                .join(screening.theater, theater)
                .where(
                        screening.movie.id.eq(movieId),
                        dateCondition
                )
                .orderBy(screening.startTime.asc())
                .fetch();

        return raw.stream()
                .map(MovieScreeningResponse.MovieScreeningInfo::withMovieEndTime)
                .toList();
    }

    private BooleanExpression withInTime(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay().plusHours(5);
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().plusHours(2);

        return screening.startTime.between(startOfDay, endOfDay);
    }
}