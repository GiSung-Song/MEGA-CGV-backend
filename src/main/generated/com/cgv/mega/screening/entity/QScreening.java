package com.cgv.mega.screening.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QScreening is a Querydsl query type for Screening
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QScreening extends EntityPathBase<Screening> {

    private static final long serialVersionUID = 1232979510L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QScreening screening = new QScreening("screening");

    public final com.cgv.mega.common.entity.QBaseTimeEntity _super = new com.cgv.mega.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> endTime = createDateTime("endTime", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.cgv.mega.movie.entity.QMovie movie;

    public final ListPath<ScreeningSeat, QScreeningSeat> screeningSeats = this.<ScreeningSeat, QScreeningSeat>createList("screeningSeats", ScreeningSeat.class, QScreeningSeat.class, PathInits.DIRECT2);

    public final NumberPath<Integer> sequence = createNumber("sequence", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> startTime = createDateTime("startTime", java.time.LocalDateTime.class);

    public final com.cgv.mega.theater.entity.QTheater theater;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QScreening(String variable) {
        this(Screening.class, forVariable(variable), INITS);
    }

    public QScreening(Path<? extends Screening> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QScreening(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QScreening(PathMetadata metadata, PathInits inits) {
        this(Screening.class, metadata, inits);
    }

    public QScreening(Class<? extends Screening> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.movie = inits.isInitialized("movie") ? new com.cgv.mega.movie.entity.QMovie(forProperty("movie")) : null;
        this.theater = inits.isInitialized("theater") ? new com.cgv.mega.theater.entity.QTheater(forProperty("theater")) : null;
    }

}

