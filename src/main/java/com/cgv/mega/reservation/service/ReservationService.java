package com.cgv.mega.reservation.service;

import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.payment.service.PaymentService;
import com.cgv.mega.reservation.dto.*;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.enums.ReservationStatus;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import com.cgv.mega.reservation.repository.ReservationQueryRepository;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.screening.service.ScreeningSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationQueryRepository reservationQueryRepository;
    private final ReservationGroupRepository reservationGroupRepository;
    private final ScreeningSeatRepository screeningSeatRepository;
    private final ScreeningSeatService screeningSeatService;
    private final ScreeningRepository screeningRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentService paymentService;

    // 예약 (결제하기 버튼 클릭 후 -> 결제창 열었을 떄 예약 + 결제 생성)
    @Transactional
    public ReservationGroup createReservation(Long userId, Long screeningId, ReservationRequest request) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCREENING_NOT_FOUND));

        // 예약 가능한지 검증
        screening.validateReservable(LocalDateTime.now());

        // row lock
        List<ScreeningSeat> screeningSeats
                = screeningSeatRepository.findByIdInAndScreeningIdForUpdate(request.screeningSeatIds(), screeningId);

        if (screeningSeats.size() != request.screeningSeatIds().size()) {
            throw new CustomException(ErrorCode.SCREENING_SEAT_NOT_FOUND);
        }

        // redis ttl로 현재 사용자가 hold한 좌석이 맞는지 검증 아니면 다른 사람이 hold한 좌석이므로 throw
        List<String> keys = request.screeningSeatIds().stream()
                .map(s -> "seat:" + s.toString())
                .toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        for (Object value : values) {
            if (value == null || !value.toString().equals(userId.toString())) {
                throw new CustomException(ErrorCode.SCREENING_SEAT_NOT_AVAILABLE);
            }
        }

        // 좌석 상태 변경
        screeningSeatService.reserveScreeningSeat(screeningSeats);

        // reservation + reservation group 생성 및 저장
        ReservationGroup reservationGroup = ReservationGroup.createReservationGroup(userId);
        for (ScreeningSeat screeningSeat : screeningSeats) {
            reservationGroup.addReservation(screeningSeat);
        }

        ReservationGroup saved = reservationGroupRepository.save(reservationGroup);

        // redis ttl 삭제
        eventPublisher.publishEvent(new DeleteScreeningSeatKeyEvent(keys));

        return saved;
    }

    // 예약 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<ReservationListResponse> getReservationList(Long userId, Pageable pageable) {
        Page<ReservationListDto> reservationList = reservationQueryRepository.getReservationList(
                userId, pageable
        );

        Page<ReservationListResponse> content
                = reservationList.map(ReservationListResponse::from);

        return PageResponse.from(content);
    }

    // 상영 취소로 인한 예약 전체 취소
    @Transactional
    public void cancelReservationByScreeningCancel(ReservationGroup reservationGroup) {
        // 결제 상태면 결제 취소 및 환불 처리
        if (reservationGroup.getStatus() == ReservationStatus.PAID) {
            paymentService.cancelPaymentByAdmin(reservationGroup);
        }

        reservationGroup.cancel();
    }

    // 예약 취소
    @Transactional
    public void cancelReservation(Long userId, Long reservationGroupId) {
        ReservationGroup reservationGroup = reservationGroupRepository.findByIdAndUserId(reservationGroupId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        // 예약 취소가 가능한 시간인지 검증
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = screeningRepository.findScreeningStartTime(reservationGroupId, userId);

        long minutes = Duration.between(now, startTime).toMinutes();

        if (minutes < 10) {
            throw new CustomException(ErrorCode.RESERVATION_CANCEL_NOT_ALLOWED);
        }

        // 결제 상태면 결제 취소 및 환불 처리
        if (reservationGroup.getStatus() == ReservationStatus.PAID) {
            paymentService.cancelPayment(reservationGroup, minutes);
        }

        // 예약 취소 및 좌석 상태 변경
        reservationGroup.cancelAndReleaseSeats();
    }

    // 예약 상세 조회 (결제 도메인 후)
    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetail(Long userId, Long reservationGroupId) {
        ReservationDetailDto dto = reservationQueryRepository.getReservationDetail(userId, reservationGroupId);

        if (dto == null) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        return ReservationDetailResponse.from(dto);
    }
}
