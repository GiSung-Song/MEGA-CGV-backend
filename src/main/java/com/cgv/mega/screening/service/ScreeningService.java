package com.cgv.mega.screening.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.screening.dto.RegisterScreeningRequest;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.theater.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;

    // 상영 추가
    public void registerScreening(RegisterScreeningRequest request) {
        // 시작 시간이 이전이면 throw
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_SCREENING_START_TIME);
        }

        // 영화 없으면 throw

        // 상영관 없으면 throw

        // 상영 회차 조회

        // 상영 저장
    }

    // 상영 삭제

    // 상영중인 영화 목록

    // 특정 영화의 상영 목록


}
