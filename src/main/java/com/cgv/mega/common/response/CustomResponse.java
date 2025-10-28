package com.cgv.mega.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class CustomResponse<T> {
    /** HTTP 상태 코드 (예: 200, 201 등) */
    private final int status;

    /** 응답 메시지 (예: "성공", "회원가입 성공" 등) */
    private final String message;

    /** 응답 데이터 (없을 경우 null) */
    private final T data;

    /**
     * 상태코드와 데이터만 받는 생성자 (메시지는 기본값 "성공")
     * @param status HTTP 상태 코드
     * @param data   응답 데이터
     */
    public CustomResponse(int status, T data) {
        this.status = status;
        this.message = "성공";
        this.data = data;
    }

    /**
     * 상태코드, 메시지, 데이터 모두 받는 생성자
     * @param status  HTTP 상태 코드
     * @param message 응답 메시지
     * @param data    응답 데이터
     */
    public CustomResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * HTTP 상태 코드, 메시지, 데이터로 객체 생성
     */
    public static <T> CustomResponse<T> of(HttpStatus status, String message, T data) {
        return new CustomResponse<>(status.value(), message, data);
    }

    /**
     * HTTP 상태 코드, 데이터로 객체 생성 (메시지는 기본값 "성공")
     */
    public static <T> CustomResponse<T> of(HttpStatus status, T data) {
        return new CustomResponse<>(status.value(), data);
    }

    /**
     * HTTP 상태 코드로 객체 생성 (데이터 null, 메시지 "성공")
     */
    public static <T> CustomResponse<T> of(HttpStatus status) {
        return new CustomResponse<>(status.value(), null);
    }

    /**
     * 데이터로 객체 생성 (상태 코드 200 OK, 메시지 "성공")
     */
    public static <T> CustomResponse<T> of(T data) {
        return new CustomResponse<>(HttpStatus.OK.value(), data);
    }

    /**
     * 기본 성공 객체 생성 (상태 코드 200 OK, 데이터 null, 메시지 "성공")
     */
    public static <T> CustomResponse<T> of() {
        return new CustomResponse<>(HttpStatus.OK.value(), null);
    }
}
