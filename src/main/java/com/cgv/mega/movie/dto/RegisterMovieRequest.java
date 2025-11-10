package com.cgv.mega.movie.dto;

import com.cgv.mega.common.enums.MovieType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.Set;

public record RegisterMovieRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Length(max = 100, message = "제목은 최대 100자입니다.")
        String title,

        @NotNull(message = "상영시간(분 단위)은 필수입니다.")
        @Min(0)
        Integer duration,

        @NotBlank(message = "영화 소개는 필수입니다.")
        String description,

        @NotBlank(message = "영화 포스터 URL은 필수입니다.")
        @Length(max = 500, message = "영화 포스터 URL은 최대 500자입니다.")
        String posterUrl,

        @NotEmpty(message = "최소 1개 이상의 타입이 필요합니다.")
        Set<MovieType> types,

        @NotEmpty(message = "최소 1개 이상의 장르가 필요합니다.")
        Set<Long> genreIds
) {
}
