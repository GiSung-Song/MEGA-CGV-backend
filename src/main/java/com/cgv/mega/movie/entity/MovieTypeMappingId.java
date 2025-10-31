package com.cgv.mega.movie.entity;

import com.cgv.mega.common.enums.MovieType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
public class MovieTypeMappingId implements Serializable {
    private Long movie;
    private MovieType type;
}