package com.cgv.mega.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
