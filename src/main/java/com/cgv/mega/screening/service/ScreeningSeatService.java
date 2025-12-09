package com.cgv.mega.screening.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.screening.dto.ScreeningSeatHoldDto;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class ScreeningSeatService {

    private final ScreeningSeatRepository screeningSeatRepository;
    private final ScreeningRepository screeningRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 해당 좌석 상태 변경 -> 수리중 (관리자용)
    @Transactional
    public void fixingScreeningSeat(Long screeningSeatId) {
        ScreeningSeat screeningSeat = screeningSeatRepository.findById(screeningSeatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        screeningSeat.fixScreeningSeat();
    }

    // 수리 완료 (관리자용)
    @Transactional
    public void restoringScreeningSeat(Long screeningSeatId) {
        ScreeningSeat screeningSeat = screeningSeatRepository.findById(screeningSeatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        screeningSeat.restoreScreeningSeat();
    }

    // 좌석 홀드(redis ttl)
    @Transactional
    public void holdScreeningSeat(Long userId, Long screeningId, ScreeningSeatHoldDto request) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCREENING_NOT_FOUND));

        // 예약 가능한지 확인
        screening.validateReservable(LocalDateTime.now());

        Set<Long> sortIds = new TreeSet<>(request.screeningSeatIds());

        List<ScreeningSeat> screeningSeats = screeningSeatRepository.findByIdInAndScreeningId(
                sortIds, screeningId
        );

        if (screeningSeats.size() != request.screeningSeatIds().size()) {
            throw new CustomException(ErrorCode.SCREENING_SEAT_NOT_FOUND);
        }

        for (ScreeningSeat screeningSeat : screeningSeats) {
            if (screeningSeat.getStatus() != ScreeningSeatStatus.AVAILABLE) {
                throw new CustomException(ErrorCode.SCREENING_SEAT_NOT_AVAILABLE);
            }
        }

        List<String> acquiredKeys = new ArrayList<>();

        for (Long screeningSeatId : sortIds) {
            String key = "seat:" + screeningSeatId;

            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && value.toString().equals(userId.toString())) {
                continue;
            }

            Boolean success = redisTemplate
                    .opsForValue()
                    .setIfAbsent(key, userId.toString(), Duration.ofMinutes(5));

            if (Boolean.FALSE.equals(success)) {
                for (String acquiredKey : acquiredKeys) {
                    redisTemplate.delete(acquiredKey);
                }

                throw new CustomException(ErrorCode.SCREENING_SEAT_ALREADY_HOLD);
            }

            acquiredKeys.add(key);
        }
    }

    // 좌석 홀드 취소 (뒤로가기 및 좌석 변경)
    public void cancelHoldScreeningSeat(Long userId, ScreeningSeatHoldDto request) {
        if (request.screeningSeatIds().isEmpty()) return;

        for (Long screeningSeatId : request.screeningSeatIds()) {
            String key = "seat:" + screeningSeatId;

            Object value = redisTemplate.opsForValue().get(key);

            if (value != null && value.equals(String.valueOf(userId))) {
                redisTemplate.delete(key);
            }
        }
    }

    // 좌석 예약 상태로 변경
    public void reserveScreeningSeat(List<ScreeningSeat> screeningSeats) {
        for (ScreeningSeat screeningSeat : screeningSeats) {
            screeningSeat.reserveScreeningSeat();
        }
    }
}
