package com.cgv.mega.booking.service;

import com.cgv.mega.booking.dto.BookingResponse;
import com.cgv.mega.booking.dto.BuyerInfoDto;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.payment.entity.Payment;
import com.cgv.mega.payment.service.PaymentService;
import com.cgv.mega.reservation.dto.ReservationRequest;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.service.ReservationService;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    @Transactional
    public BookingResponse startBooking(Long userId, Long screeningId, ReservationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        BuyerInfoDto buyerInfoDto = new BuyerInfoDto(user.getName(), user.getPhoneNumber(), user.getEmail());

        ReservationGroup reservationGroup = reservationService.createReservation(userId, screeningId, request);
        Payment payment = paymentService.createPayment(reservationGroup, buyerInfoDto);

        return new BookingResponse(reservationGroup.getId(), payment.getMerchantUid(), payment.getExpectedAmount());
    }
}