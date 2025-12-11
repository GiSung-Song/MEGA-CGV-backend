package com.cgv.mega.auth.dto;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.exception.CustomException;
import io.jsonwebtoken.Claims;

import java.util.Map;

public record JwtPayloadDto(
        Long userId,
        String email,
        String name,
        Role role
) {
    public Map<String, Object> toClaims() {
        validate();

        return Map.of(
                "email", email,
                "name", name,
                "role", role.name()
        );
    }

    public void validate() {
        if (userId == null) throw new CustomException(ErrorCode.JWT_USER_ID_IS_NULL);
        if (email == null) throw new CustomException(ErrorCode.JWT_NAME_IS_NULL);
        if (name == null) throw new CustomException(ErrorCode.JWT_NAME_IS_NULL);
        if (role == null) throw new CustomException(ErrorCode.JWT_ROLE_IS_NULL);
    }

    public static JwtPayloadDto fromClaims(Claims claims) {
        return new JwtPayloadDto(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("name", String.class),
                Role.valueOf(claims.get("role", String.class)));
    }
}
