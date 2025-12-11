package com.cgv.mega.screening.dto;

import com.cgv.mega.screening.enums.ScreeningStatus;

import java.time.LocalDateTime;
import java.util.List;

public record MovieScreeningResponse(
    List<MovieInfo> movieInfoList
) {
    public record MovieInfo(
            Long screeningId,
            Long theaterId,
            String theaterName,
            Long remainSeatCount,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer sequence,
            ScreeningStatus status,
            boolean reservable
    ) {}

    public static MovieInfo toMovieInfo(MovieScreeningInfoDto dto) {
        boolean reservable = (dto.screeningStatus() == ScreeningStatus.SCHEDULED) &&
                LocalDateTime.now().isBefore(dto.startTime().minusMinutes(10));

        return new MovieInfo(
                dto.screeningId(),
                dto.theaterId(),
                dto.theaterName(),
                dto.remainSeat(),
                dto.startTime(),
                dto.endTime().minusMinutes(10),
                dto.sequence(),
                dto.screeningStatus(),
                reservable
        );
    }

    public static MovieScreeningResponse from(List<MovieScreeningInfoDto> dtoList) {
        List<MovieInfo> infoList = dtoList.stream()
                .map(MovieScreeningResponse::toMovieInfo)
                .toList();

        return new MovieScreeningResponse(infoList);
    }
}
