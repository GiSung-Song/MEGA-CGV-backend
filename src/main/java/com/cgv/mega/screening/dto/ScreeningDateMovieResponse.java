package com.cgv.mega.screening.dto;

import java.util.List;

public record ScreeningDateMovieResponse(
        List<MovieInfo> movieInfos
) {
    public record MovieInfo(
            Long movieId,
            String title,
            String posterUrl
    ) { }
}