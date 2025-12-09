package com.cgv.mega.reservation.controller;

import com.cgv.mega.booking.dto.BookingResponse;
import com.cgv.mega.booking.service.BookingService;
import com.cgv.mega.common.dto.PageResponse;
import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.common.security.CustomUserDetails;
import com.cgv.mega.reservation.dto.ReservationDetailResponse;
import com.cgv.mega.reservation.dto.ReservationListResponse;
import com.cgv.mega.reservation.dto.ReservationRequest;
import com.cgv.mega.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final BookingService bookingService;

    @PostMapping("/{screeningId}")
    public ResponseEntity<CustomResponse<BookingResponse>> reservation(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("screeningId") Long screeningId,
            @RequestBody @Valid ReservationRequest request
    ) {
        BookingResponse bookingResponse = bookingService.startBooking(user.id(), screeningId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.of(HttpStatus.CREATED, bookingResponse));
    }

    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<ReservationListResponse>>> getReservationList(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<ReservationListResponse> response
                = reservationService.getReservationList(user.id(), pageable);

        return ResponseEntity.ok(CustomResponse.of(response));
    }

    @DeleteMapping("{reservationGroupId}")
    public ResponseEntity<CustomResponse<Void>> cancelReservation(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("reservationGroupId") Long reservationGroupId
    ) {
        reservationService.cancelReservation(user.id(), reservationGroupId);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @GetMapping("/{reservationGroupId}")
    public ResponseEntity<CustomResponse<ReservationDetailResponse>> getReservationDetail(
            @PathVariable("reservationGroupId") Long reservationGroupId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ReservationDetailResponse response = reservationService.getReservationDetail(user.id(), reservationGroupId);

        return ResponseEntity.ok(CustomResponse.of(response));
    }
}
