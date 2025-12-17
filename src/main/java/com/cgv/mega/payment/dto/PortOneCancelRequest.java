package com.cgv.mega.payment.dto;

public record PortOneCancelRequest(
        Long amount,
        String reason
) {
}