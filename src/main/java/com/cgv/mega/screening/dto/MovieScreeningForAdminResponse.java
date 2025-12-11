package com.cgv.mega.screening.dto;

import com.cgv.mega.screening.enums.ScreeningStatus;

import java.time.LocalDateTime;
import java.util.List;

public record MovieScreeningForAdminResponse(
        List<MovieScreeningInfo> movieScreeningInfos
) {
    public record MovieScreeningInfo(
            Long screeningId,
            Long theaterId,
            String theaterName,
            Long remainSeat,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int sequence,
            ScreeningStatus status
    ) {
        public MovieScreeningInfo withMovieEndTime() {
            return new MovieScreeningInfo(
                    screeningId,
                    theaterId,
                    theaterName,
                    remainSeat,
                    startTime,
                    endTime.minusMinutes(10),
                    sequence,
                    status
            );
        }
    }
}
