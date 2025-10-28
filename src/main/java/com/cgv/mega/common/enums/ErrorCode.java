package com.cgv.mega.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR("INTERNAL_ERROR", "서버에서 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Jwt Payload
    JWT_USER_ID_IS_NULL("JWT_USER_ID_IS_NULL", "JWT 생성 중 사용자 ID가 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_EMAIL_IS_NULL("JWT_EMAIL_IS_NULL", "JWT 생성 중 이메일이 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_NAME_IS_NULL("JWT_NAME_IS_NULL", "JWT 생성 중 이름이 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_ROLE_IS_NULL("JWT_ROLE_IS_NULL", "JWT 생성 중 ROLE이 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Jwt Token
    JWT_TOKEN_INVALID("JWT_TOKEN_INVALID", "JWT 토큰이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_EXPIRED("JWT_TOKEN_EXPIRED", "JWT 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}