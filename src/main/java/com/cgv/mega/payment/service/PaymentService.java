package com.cgv.mega.payment.service;

import com.cgv.mega.booking.dto.BuyerInfoDto;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.payment.dto.*;
import com.cgv.mega.payment.entity.Payment;
import com.cgv.mega.payment.enums.PaymentStatus;
import com.cgv.mega.payment.repository.PaymentRepository;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationGroupRepository reservationGroupRepository;
    private final PortOneClient portOneClient;

    private static final String ALPHA_NUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // 결제 요청 준비 데이터 조회
    @Transactional
    public Payment createPayment(ReservationGroup reservationGroup, BuyerInfoDto dto) {
        BigDecimal expectedAmount = BigDecimal.valueOf(reservationGroup.getTotalPrice());

        Payment payment = Payment.createPayment(
                reservationGroup,
                dto.name(),
                dto.phoneNumber(),
                dto.email(),
                createPaymentId(reservationGroup.getId()),
                expectedAmount
        );

        Payment saved = paymentRepository.save(payment);

        return saved;
    }

    // 결제 검증
    @Transactional
    public void verifyAndCompletePayment(Long userId, PaymentCompleteRequest request) {
        ReservationGroup reservationGroup = reservationGroupRepository.findByIdAndUserId(request.reservationGroupId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        Payment payment = paymentRepository.findByPaymentId(request.paymentId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 검증 완료된 상태면 return
        if (payment.isFinalized()) {
            return;
        }

        // 해당 예약에 대한 결제 내역인지 검증
        if (!payment.getReservationGroup().getId().equals(reservationGroup.getId())) {
            throw new CustomException(ErrorCode.PAYMENT_INFO_MISMATCH);
        }

        // 포트원 서버로부터 결제 조회 API 호출
        PortOnePaymentResponse response = portOneClient.getPaymentInfo(request.paymentId());

        // paymentId (거래 ID) 일치 검증
        if (!response.id().equals(request.paymentId())) {
            markFail(payment, reservationGroup, "payment_id mismatch");
            throw new CustomException(ErrorCode.PAYMENT_INFO_MISMATCH);
        }

        // 금액 검증
        BigDecimal paidAmount = response.amount().total();
        if (paidAmount.compareTo(payment.getExpectedAmount()) != 0) {
            markFail(payment, reservationGroup, "amount mismatch");
            throw new CustomException(ErrorCode.PAYMENT_INFO_MISMATCH);
        }

        // 결제 상태 검증
        if (!"PAID".equalsIgnoreCase(response.status())) {
            markFail(payment, reservationGroup, "status: " + response.status());
            throw new CustomException(ErrorCode.PAYMENT_INFO_MISMATCH);
        }

        // 결제 상태 변경 및 데이터 추가
        updatePaymentSuccess(payment, response);

        // 예약 상태 변경
        reservationGroup.successReservation();
    }

    // 결제 취소
    @Transactional
    public void cancelPayment(ReservationGroup reservationGroup, long minutes) {
        Payment payment = paymentRepository.findByReservationGroupId(reservationGroup.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }

        // 환불 금액 계산
        BigDecimal refundAmount = calculateRefundAmount(minutes, payment.getPaidAmount());
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.RESERVATION_CANCEL_NOT_ALLOWED);
        }

        executeRefund(payment, refundAmount, "사용자 예약 취소로 인한 환불");
    }

    @Transactional
    public void cancelPaymentByAdmin(ReservationGroup reservationGroup) {
        Payment payment = paymentRepository.findByReservationGroupId(reservationGroup.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }

        BigDecimal refundAmount = payment.getPaidAmount();

        executeRefund(payment, refundAmount, "상영 취소로 인한 전액 환불");
    }

    private void executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        // 환불 API 요청
        PortOneCancelRequest request = new PortOneCancelRequest(refundAmount.longValueExact(), reason);
        RefundResult result = portOneClient.refundPayment(payment.getPaymentId(), request);

        if (result.isFailure()) {
            payment.failedPayment(result.reason());
            throw new CustomException(ErrorCode.PAYMENT_REFUND_FAILED);
        }

        // 결제 상태 변경
        BigDecimal cancelledAmount = result.cancelledAmount();
        payment.cancelPayment(cancelledAmount, reason);
    }

    // 결제번호 (paymentId 생성)
    private String createPaymentId(Long reservationGroupId) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        return new StringBuilder()
                .append("megacgv-")
                .append(timestamp)
                .append("-")
                .append(reservationGroupId)
                .append("-")
                .append(randomString(10))
                .toString();
    }

    private String randomString(int length) {
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUM.charAt(random.nextInt(ALPHA_NUM.length())));
        }
        return sb.toString();
    }

    private void markFail(Payment payment, ReservationGroup reservationGroup, String reason) {
        if (payment.isFinalized()) {
            return;
        }

        // 결제 상태 실패로 변경
        payment.failedPayment(reason);

        // 예약 상태 실패로 변경 및 좌석 반환
        reservationGroup.cancelAndReleaseSeats();
    }

    private void updatePaymentSuccess(Payment payment, PortOnePaymentResponse response) {
        payment.successPayment(
                response.id(),
                response.amount().total(),
                response.method() != null ? response.method().provider() : null,
                response.method() != null ? response.method().type() : null,
                response.method() != null ? response.method().cardName() : null,
                response.method() != null ? response.method().cardQuota() : null,
                parseDateTime(response.statusChangedAt())
        );
    }

    private LocalDateTime parseDateTime(String rfc3339) {
        if (rfc3339 == null) return null;
        return OffsetDateTime.parse(rfc3339).toLocalDateTime();
    }

    private BigDecimal calculateRefundAmount(long minutes, BigDecimal paidAmount) {
        if (minutes >= 24 * 60) {
            return paidAmount.multiply(BigDecimal.valueOf(1.0));
        } else if (minutes >= 60) {
            return paidAmount.multiply(BigDecimal.valueOf(0.3));
        } else if (minutes >= 10) {
            return paidAmount.multiply(BigDecimal.valueOf(0.1));
        } else {
            return BigDecimal.ZERO;
        }
    }
}
