package com.cgv.mega.movie.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMovieTypeMapping is a Querydsl query type for MovieTypeMapping
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMovieTypeMapping extends EntityPathBase<MovieTypeMapping> {

    private static final long serialVersionUID = -1275373474L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMovieTypeMapping movieTypeMapping = new QMovieTypeMapping("movieTypeMapping");

    public final QMovie movie;

    public final EnumPath<com.cgv.mega.movie.enums.MovieType> type = createEnum("type", com.cgv.mega.movie.enums.MovieType.class);

    public QMovieTypeMapping(String variable) {
        this(MovieTypeMapping.class, forVariable(variable), INITS);
    }

    public QMovieTypeMapping(Path<? extends MovieTypeMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMovieTypeMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMovieTypeMapping(PathMetadata metadata, PathInits inits) {
        this(MovieTypeMapping.class, metadata, inits);
    }

    public QMovieTypeMapping(Class<? extends MovieTypeMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.movie = inits.isInitialized("movie") ? new QMovie(forProperty("movie")) : null;
    }

}

