package com.cgv.mega.payment.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.common.security.CustomUserDetails;
import com.cgv.mega.payment.dto.PaymentCompleteRequest;
import com.cgv.mega.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/complete")
    public ResponseEntity<CustomResponse<Void>> completePayments(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PaymentCompleteRequest request) {
        paymentService.verifyAndCompletePayment(user.id(), request);

        return ResponseEntity.ok(CustomResponse.of());
    }
}