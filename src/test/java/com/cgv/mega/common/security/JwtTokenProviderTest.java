package com.cgv.mega.common.security;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.auth.enums.TokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {
    private JwtTokenProvider jwtTokenProvider;

    private JwtPayloadDto dto;

    @BeforeEach
    void setUp() {
        dto = new JwtPayloadDto(1L, "a@b.com", "user", Role.USER);

        String secretKey = "sdapifjpi324jpifqhidpashf803h280i1fhidshaf80h340281q";
        Long accessExpiration = 1000 * 60 * 1L;
        Long refreshExpiration = 1000 * 60 * 60L;

        jwtTokenProvider = new JwtTokenProvider(secretKey, accessExpiration, refreshExpiration);
    }

    @Test
    void ACCESS_TOKEN_생성_및_PARSING_성공() {
        String accessToken = jwtTokenProvider.generateAccessToken(dto);
        JwtPayloadDto jwtPayloadDto = jwtTokenProvider.parseToken(accessToken);

        assertThat(dto.userId()).isEqualTo(jwtPayloadDto.userId());
        assertThat(dto.name()).isEqualTo(jwtPayloadDto.name());
        assertThat(dto.email()).isEqualTo(jwtPayloadDto.email());
        assertThat(dto.role()).isEqualTo(jwtPayloadDto.role());
    }

    @Test
    void REFRESH_TOKEN_생성_및_PARSING_성공() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(dto);
        JwtPayloadDto jwtPayloadDto = jwtTokenProvider.parseToken(refreshToken);

        assertThat(dto.userId()).isEqualTo(jwtPayloadDto.userId());
        assertThat(dto.name()).isEqualTo(jwtPayloadDto.name());
        assertThat(dto.email()).isEqualTo(jwtPayloadDto.email());
        assertThat(dto.role()).isEqualTo(jwtPayloadDto.role());
    }

    @Test
    void 토큰_만료_시간_조회() {
        String accessToken = jwtTokenProvider.generateAccessToken(dto);

        Date expiration = jwtTokenProvider.getTokenExpiration(accessToken);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void 토큰_정상() {
        String accessToken = jwtTokenProvider.generateAccessToken(dto);
        TokenStatus status = jwtTokenProvider.getTokenStatus(accessToken);

        assertThat(status).isEqualTo(TokenStatus.VALID);
    }

    @Test
    void 토큰_만료() {
        String secretKey = "sdapifjpi324jpifqhidpashf803h280i1fhidshaf80h340281q";
        Long accessExpiration = 10 * 1L;
        Long refreshExpiration = 10 * 1L;

        JwtTokenProvider shortProvider = new JwtTokenProvider(secretKey, accessExpiration, refreshExpiration);

        String accessToken = shortProvider.generateAccessToken(dto);

        TokenStatus status = shortProvider.getTokenStatus(accessToken);

        assertThat(status).isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    void 유효하지_않은_토큰() {
        String accessToken = jwtTokenProvider.generateAccessToken(dto);
        String invalidToken = accessToken + "asdfa";

        TokenStatus status = jwtTokenProvider.getTokenStatus(invalidToken);

        assertThat(status).isEqualTo(TokenStatus.INVALID);
    }

    @Test
    void REFRESH_토큰_만료_시간_조회() {
        assertThat(jwtTokenProvider.getRefreshTokenExpiration()).isEqualTo(1000L * 60 * 60);
    }

    @Test
    void 토큰_해시_변환_성공() {
        String accessToken = jwtTokenProvider.generateAccessToken(dto);
        Optional<String> hash = jwtTokenProvider.tokenToHash(accessToken);

        assertThat(hash).isPresent();
        assertThat(hash.get()).hasSize(64);
    }
}