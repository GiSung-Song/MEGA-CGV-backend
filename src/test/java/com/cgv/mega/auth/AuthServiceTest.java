package com.cgv.mega.auth;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.auth.dto.LoginRequest;
import com.cgv.mega.auth.dto.TokenResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.auth.enums.TokenStatus;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.common.security.JwtTokenProvider;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisTemplate<String, String> redisTemplate;

    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createUser("user", "a@b.com", "encodedPassword", "01012341234");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Nested
    class 로그인_테스트 {

        @Test
        void 로그인_성공() {
            LoginRequest request = new LoginRequest("a@b.com", "rawPassword");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtTokenProvider.generateAccessToken(any(JwtPayloadDto.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(JwtPayloadDto.class))).willReturn("refresh-token");
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(600000L);

            ValueOperations<String, String> ops = mock(ValueOperations.class);
            given(redisTemplate.opsForValue()).willReturn(ops);

            TokenResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            verify(ops).set(eq("refresh:" + user.getId()), eq("refresh-token"), eq(600000L), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        void 이메일로_조회_실패_401반환() {
            LoginRequest request = new LoginRequest("aa@bb.com", "rawPassword");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAIL);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }

        @Test
        void 비밀번호_오류_실패_401반환() {
            LoginRequest request = new LoginRequest("a@b.com", "wrongPassword");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAIL);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }
    }

    @Nested
    class 로그아웃_테스트 {

        @Test
        void 로그아웃_성공() {
            String accessToken = "access-token";

            Date future = Date.from(Instant.now().plusSeconds(60));
            given(jwtTokenProvider.getTokenExpiration(accessToken)).willReturn(future);
            given(jwtTokenProvider.tokenToHash(accessToken)).willReturn(Optional.of("hashed-token"));

            ValueOperations<String, String> ops = mock(ValueOperations.class);
            given(redisTemplate.opsForValue()).willReturn(ops);

            authService.logout(accessToken);

            verify(ops).set(eq("hashed-token"), eq("logout"), anyLong(), eq(TimeUnit.MILLISECONDS));
        }
    }

    @Nested
    class 토큰_재발급_테스트 {

        @Test
        void 토큰_재발급_성공() {
            String refreshToken = "refresh-token";
            JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

            given(jwtTokenProvider.getTokenStatus(refreshToken)).willReturn(TokenStatus.VALID);
            given(jwtTokenProvider.parseToken(refreshToken)).willReturn(jwtPayloadDto);
            given(userRepository.findById(jwtPayloadDto.userId())).willReturn(Optional.of(user));

            ValueOperations<String, String> ops = mock(ValueOperations.class);
            given(redisTemplate.opsForValue()).willReturn(ops);

            given(ops.get("refresh:" + user.getId())).willReturn("refresh-token");
            given(jwtTokenProvider.generateAccessToken(jwtPayloadDto)).willReturn("access-token");

            TokenResponse tokenResponse = authService.refreshToken(refreshToken);

            assertThat(tokenResponse.accessToken()).isEqualTo("access-token");
            assertThat(tokenResponse.refreshToken()).isNull();;
        }

        @Test
        void 리프레시_토큰_null_401반환() {
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JWT_TOKEN_INVALID);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }

        @Test
        void 리프레시_토큰_유효하지_않은_경우_401반환() {
            String refreshToken = "refresh-token";
            given(jwtTokenProvider.getTokenStatus(refreshToken)).willReturn(TokenStatus.INVALID);

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JWT_TOKEN_INVALID);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }

        @Test
        void 사용자_없음_404반환() {
            String refreshToken = "refresh-token";
            JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

            given(jwtTokenProvider.getTokenStatus(refreshToken)).willReturn(TokenStatus.VALID);
            given(jwtTokenProvider.parseToken(refreshToken)).willReturn(jwtPayloadDto);
            given(userRepository.findById(user.getId())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 저장된_리프레시_토큰_null_401반환() {
            String refreshToken = "refresh-token";
            JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

            given(jwtTokenProvider.getTokenStatus(refreshToken)).willReturn(TokenStatus.VALID);
            given(jwtTokenProvider.parseToken(refreshToken)).willReturn(jwtPayloadDto);
            given(userRepository.findById(jwtPayloadDto.userId())).willReturn(Optional.of(user));

            ValueOperations<String, String> ops = mock(ValueOperations.class);
            given(redisTemplate.opsForValue()).willReturn(ops);

            given(ops.get("refresh:" + user.getId())).willReturn(null);

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JWT_TOKEN_INVALID);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }

        @Test
        void 저장된_리프레시_토큰이랑_다름_401반환() {
            String refreshToken = "refresh-token";
            JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

            given(jwtTokenProvider.getTokenStatus(refreshToken)).willReturn(TokenStatus.VALID);
            given(jwtTokenProvider.parseToken(refreshToken)).willReturn(jwtPayloadDto);
            given(userRepository.findById(jwtPayloadDto.userId())).willReturn(Optional.of(user));

            ValueOperations<String, String> ops = mock(ValueOperations.class);
            given(redisTemplate.opsForValue()).willReturn(ops);

            given(ops.get("refresh:" + user.getId())).willReturn("refresh-ttoken");

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JWT_TOKEN_INVALID);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }
    }
}