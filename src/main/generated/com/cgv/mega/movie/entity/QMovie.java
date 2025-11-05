package com.cgv.mega.movie.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMovie is a Querydsl query type for Movie
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMovie extends EntityPathBase<Movie> {

    private static final long serialVersionUID = 867272182L;

    public static final QMovie movie = new QMovie("movie");

    public final com.cgv.mega.common.entity.QBaseTimeEntity _super = new com.cgv.mega.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Integer> duration = createNumber("duration", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<MovieGenre, QMovieGenre> movieGenres = this.<MovieGenre, QMovieGenre>createList("movieGenres", MovieGenre.class, QMovieGenre.class, PathInits.DIRECT2);

    public final ListPath<MovieTypeMapping, QMovieTypeMapping> movieTypes = this.<MovieTypeMapping, QMovieTypeMapping>createList("movieTypes", MovieTypeMapping.class, QMovieTypeMapping.class, PathInits.DIRECT2);

    public final StringPath posterUrl = createString("posterUrl");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMovie(String variable) {
        super(Movie.class, forVariable(variable));
    }

    public QMovie(Path<? extends Movie> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMovie(PathMetadata metadata) {
        super(Movie.class, metadata);
    }

}

