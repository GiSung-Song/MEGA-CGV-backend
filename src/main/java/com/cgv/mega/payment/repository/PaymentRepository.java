package com.cgv.mega.payment.repository;

import com.cgv.mega.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByReservationGroupId(Long reservationGroupId);
}
