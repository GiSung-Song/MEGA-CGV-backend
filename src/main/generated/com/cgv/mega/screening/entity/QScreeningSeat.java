package com.cgv.mega.screening.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QScreeningSeat is a Querydsl query type for ScreeningSeat
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QScreeningSeat extends EntityPathBase<ScreeningSeat> {

    private static final long serialVersionUID = 743112027L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QScreeningSeat screeningSeat = new QScreeningSeat("screeningSeat");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QScreening screening;

    public final com.cgv.mega.seat.entity.QSeat seat;

    public final EnumPath<com.cgv.mega.screening.enums.ScreeningSeatStatus> status = createEnum("status", com.cgv.mega.screening.enums.ScreeningSeatStatus.class);

    public QScreeningSeat(String variable) {
        this(ScreeningSeat.class, forVariable(variable), INITS);
    }

    public QScreeningSeat(Path<? extends ScreeningSeat> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QScreeningSeat(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QScreeningSeat(PathMetadata metadata, PathInits inits) {
        this(ScreeningSeat.class, metadata, inits);
    }

    public QScreeningSeat(Class<? extends ScreeningSeat> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.screening = inits.isInitialized("screening") ? new QScreening(forProperty("screening"), inits.get("screening")) : null;
        this.seat = inits.isInitialized("seat") ? new com.cgv.mega.seat.entity.QSeat(forProperty("seat"), inits.get("seat")) : null;
    }

}

