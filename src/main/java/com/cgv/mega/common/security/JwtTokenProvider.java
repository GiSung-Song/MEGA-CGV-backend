package com.cgv.mega.common.security;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.auth.enums.TokenStatus;
import com.cgv.mega.common.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Long accessTokenExpiration;
    private final Long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secretKey}") String secretKey,
                            @Value("${jwt.access.expiration}") Long accessTokenExpiration,
                            @Value("${jwt.refresh.expiration}") Long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // Access Token 생성
    public String generateAccessToken(JwtPayloadDto jwtPayload) {
        return generateToken(jwtPayload, accessTokenExpiration);
    }

    // Refresh Token 생성
    public String generateRefreshToken(JwtPayloadDto jwtPayload) {
        return generateToken(jwtPayload, refreshTokenExpiration);
    }

    private String generateToken(JwtPayloadDto jwtPayload, Long expiration) {
        return Jwts.builder()
                .subject(String.valueOf(jwtPayload.userId()))
                .claims(jwtPayload.toClaims())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .issuedAt(new Date())
                .signWith(secretKey)
                .compact();
    }

    // 토큰 만료 시간 조회
    public Date getTokenExpiration(String token) {
        return parseTokenToClaims(token).getExpiration();
    }

    public TokenStatus getTokenStatus(String token) {
        try {
            parseTokenToClaims(token);
            return TokenStatus.VALID;
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.JWT_TOKEN_EXPIRED) {
                return TokenStatus.EXPIRED;
            }

            return TokenStatus.INVALID;
        }
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // Token 파싱
    public JwtPayloadDto parseToken(String token) {
        try {
            Claims claims = parseTokenToClaims(token);

            return JwtPayloadDto.fromClaims(claims);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_INVALID);
        }
    }

    // 토큰 문자열을 SHA-256 해시로 변환
    public Optional<String> tokenToHash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(accessToken.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return Optional.of(hexString.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // JWT 토큰 파싱
    private Claims parseTokenToClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_INVALID);
        }
    }
}
