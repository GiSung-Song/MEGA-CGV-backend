package com.cgv.mega.movie.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
public class MovieGenreId implements Serializable {
    private Long movie;
    private Long genre;
}