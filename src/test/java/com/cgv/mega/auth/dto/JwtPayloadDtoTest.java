package com.cgv.mega.auth.dto;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPayloadDtoTest {

    @Test
    void DTO_생성_성공() {
        JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(1L, "a@b.com", "user", Role.USER);

        assertThat(jwtPayloadDto.userId()).isEqualTo(1L);
        assertThat(jwtPayloadDto.email()).isEqualTo("a@b.com");
        assertThat(jwtPayloadDto.name()).isEqualTo("user");
        assertThat(jwtPayloadDto.role()).isEqualTo(Role.USER);
    }

    @Test
    void DTO_생성_실패_식별자ID_null() {
        assertThatThrownBy(() -> new JwtPayloadDto(null, "a@b.com", "user", Role.USER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException exception = (CustomException) ex;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JWT_USER_ID_IS_NULL);
                });
    }

    @Test
    void DTO_생성_실패_이메일_null() {
        assertThatThrownBy(() -> new JwtPayloadDto(1L, null, "user", Role.USER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException exception = (CustomException) ex;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JWT_EMAIL_IS_NULL);
                });
    }

    @Test
    void DTO_생성_실패_이름_null() {
        assertThatThrownBy(() -> new JwtPayloadDto(1L, "a@b.com", null, Role.USER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException exception = (CustomException) ex;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JWT_NAME_IS_NULL);
                });
    }

    @Test
    void DTO_생성_실패_ROLE_null() {
        assertThatThrownBy(() -> new JwtPayloadDto(1L, "a@b.com", "user", null))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException exception = (CustomException) ex;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JWT_ROLE_IS_NULL);
                });
    }

    @Test
    void DTO_TO_CLAIMS_AND_CLAIMS_TO_DTO_성공() {
        JwtPayloadDto dto = new JwtPayloadDto(1L, "a@b.com", "user", Role.USER);
        Map<String, Object> toClaims = dto.toClaims();

        Claims claims = Jwts.claims()
                .subject(String.valueOf(dto.userId()))
                .add("email", dto.email())
                .add("name", dto.name())
                .add("role", dto.role().name())
                .build();

        JwtPayloadDto fromClaims = JwtPayloadDto.fromClaims(claims);

        assertThat(fromClaims.userId()).isEqualTo(dto.userId());
        assertThat(fromClaims.email()).isEqualTo(dto.email());
        assertThat(fromClaims.name()).isEqualTo(dto.name());
        assertThat(fromClaims.role()).isEqualTo(dto.role());
    }

}