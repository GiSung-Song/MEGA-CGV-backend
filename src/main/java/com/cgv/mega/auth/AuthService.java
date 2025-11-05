package com.cgv.mega.auth;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.auth.dto.LoginRequest;
import com.cgv.mega.auth.dto.TokenResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.TokenStatus;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.common.security.JwtTokenProvider;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    // 로그인
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAIL));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAIL);
        }

        JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());
        String accessToken = jwtTokenProvider.generateAccessToken(jwtPayloadDto);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtPayloadDto);

        String redisKey = "refresh:" + user.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, jwtTokenProvider.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);

        return new TokenResponse(accessToken, refreshToken);
    }

    // 로그아웃
    public void logout(String accessToken) {
        Date expiration = jwtTokenProvider.getTokenExpiration(accessToken);
        long expirationTime = expiration.getTime() - System.currentTimeMillis();

        jwtTokenProvider.tokenToHash(accessToken).ifPresent(hash -> {
            redisTemplate.opsForValue().set(hash, "logout", expirationTime, TimeUnit.MILLISECONDS);
        });
    }

    // 토큰 재발급
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !(jwtTokenProvider.getTokenStatus(refreshToken) == TokenStatus.VALID)) {
            throw new CustomException(ErrorCode.JWT_TOKEN_INVALID);
        }

        JwtPayloadDto jwtPayloadDto = jwtTokenProvider.parseToken(refreshToken);
        Long userId = jwtPayloadDto.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String storedRefreshToken = redisTemplate.opsForValue().get("refresh:" + userId);

        if (storedRefreshToken == null || !refreshToken.equals(storedRefreshToken)) {
            throw new CustomException(ErrorCode.JWT_TOKEN_INVALID);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(jwtPayloadDto);

        return new TokenResponse(accessToken, null);
    }
}
