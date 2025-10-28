package com.cgv.mega.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> errors
) {
    /**
     * 필드 단위 오류가 없는 에러 응답 생성
     * @param status  HTTP 상태 코드
     * @param code    에러 코드
     * @param message 에러 메시지
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(HttpStatus status, String code, String message) {
        return new ErrorResponse(status.value(), code, message, null);
    }

    /**
     * 필드 단위 오류가 있는 에러 응답 생성
     * @param status  HTTP 상태 코드
     * @param code    에러 코드
     * @param message 에러 메시지
     * @param errors  필드 오류 리스트
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(HttpStatus status, String code, String message, List<FieldError> errors) {
        return new ErrorResponse(status.value(), code, message, errors);
    }

    /**
     * 필드 단위 오류 정보 DTO
     * <p>
     * - 유효성 검증 실패 등에서 사용
     * </p>
     * @param field  오류가 발생한 필드명 (예: "email", "password")
     * @param reason 오류가 발생한 이유 (예: "필수값 누락", "형식 오류")
     */
    public record FieldError(String field, String reason) {}
}
