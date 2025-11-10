package com.cgv.mega.movie.dto;

import java.util.Set;

public record MovieInfoResponse(
        String title,
        int duration,
        String description,
        String posterUrl,
        Set<String> genres,
        Set<String> types
) {
}
