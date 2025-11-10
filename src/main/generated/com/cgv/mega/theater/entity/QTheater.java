package com.cgv.mega.theater.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTheater is a Querydsl query type for Theater
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTheater extends EntityPathBase<Theater> {

    private static final long serialVersionUID = -1379451498L;

    public static final QTheater theater = new QTheater("theater");

    public final com.cgv.mega.common.entity.QBaseTimeEntity _super = new com.cgv.mega.common.entity.QBaseTimeEntity(this);

    public final NumberPath<Integer> basePrice = createNumber("basePrice", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> totalSeat = createNumber("totalSeat", Integer.class);

    public final EnumPath<com.cgv.mega.common.enums.TheaterType> type = createEnum("type", com.cgv.mega.common.enums.TheaterType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTheater(String variable) {
        super(Theater.class, forVariable(variable));
    }

    public QTheater(Path<? extends Theater> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTheater(PathMetadata metadata) {
        super(Theater.class, metadata);
    }

}

