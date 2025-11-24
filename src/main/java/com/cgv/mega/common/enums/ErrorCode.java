package com.cgv.mega.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR("INTERNAL_ERROR", "서버에서 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Auth
    LOGIN_FAIL("LOGIN_FAIL", "아이디 혹은 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // Jwt Payload
    JWT_USER_ID_IS_NULL("JWT_USER_ID_IS_NULL", "JWT 생성 중 사용자 ID가 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_EMAIL_IS_NULL("JWT_EMAIL_IS_NULL", "JWT 생성 중 이메일이 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_NAME_IS_NULL("JWT_NAME_IS_NULL", "JWT 생성 중 이름이 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_ROLE_IS_NULL("JWT_ROLE_IS_NULL", "JWT 생성 중 ROLE이 NULL입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Jwt Token
    JWT_TOKEN_INVALID("JWT_TOKEN_INVALID", "JWT 토큰이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_EXPIRED("JWT_TOKEN_EXPIRED", "JWT 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),

    // 상영-좌석
    SEAT_NOT_FOUND("SEAT_NOT_FOUND", "좌석이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    SCREENING_SEAT_NOT_AVAILABLE("SCREENING_SEAT_NOT_AVAILABLE", "예약 불가능한 좌석입니다.", HttpStatus.CONFLICT),
    SCREENING_SEAT_NOT_RESERVED("SCREENING_SEAT_NOT_RESERVED", "예약된 좌석이 아닙니다.", HttpStatus.CONFLICT),
    SCREENING_SEAT_CANNOT_UPDATE("SCREENING_SEAT_CANNOT_UPDATE", "좌석 상태를 변경할 수 없습니다.", HttpStatus.CONFLICT),

    // 사용자
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "이미 등록된 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_PHONE_NUMBER("DUPLICATE_PHONE_NUMBER", "이미 등록된 휴대폰 번호입니다.", HttpStatus.CONFLICT),
    DUPLICATE_USER("DUPLICATE_USER", "이미 등록된 사용자입니다.", HttpStatus.CONFLICT),
    INCORRECT_PASSWORD("INCORRECT_PASSWORD", "현재 비밀번호가 맞지 않습니다.", HttpStatus.BAD_REQUEST),

    // 장르
    GENRE_NOT_FOUND("GENRE_NOT_FOUND", "장르를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 영화
    MOVIE_NOT_FOUND("MOVIE_NOT_FOUND", "영화를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MOVIE_ALREADY_DELETED("MOVIE_ALREADY_DELETED", "이미 삭제된 영화입니다.", HttpStatus.CONFLICT),
    MOVIE_ALREADY_SCREENING("MOVIE_ALREADY_SCREENING", "상영중이거나 상영된 영화는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 상영관
    THEATER_NOT_FOUND("THEATER_NOT_FOUND", "상영관을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 상영
    INVALID_SCREENING_START_TIME("INVALID_SCREENING_START_TIME", "현재 시간 이전으로는 등록할 수 없습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_THEATER_SCREENING_TIME("DUPLICATE_THEATER_SCREENING_TIME", "해당 시간에 상영 예정인 영화가 있습니다.", HttpStatus.CONFLICT),
    INVALID_SCREENING_CANCEL("INVALID_SCREENING_CANCEL", "취소할 수 없는 상태입니다.", HttpStatus.BAD_REQUEST),
    INVALID_SCREENING_END("INVALID_SCREENING_END", "완료할 수 없는 상태입니다.", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}