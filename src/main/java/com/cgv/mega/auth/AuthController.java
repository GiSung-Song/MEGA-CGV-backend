package com.cgv.mega.auth;

import com.cgv.mega.auth.dto.LoginRequest;
import com.cgv.mega.auth.dto.TokenResponse;
import com.cgv.mega.common.response.CustomResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<CustomResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokenResponse = authService.login(request);

        // Refresh Token 쿠키 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenResponse.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(14 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(CustomResponse.of(tokenResponse));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<CustomResponse<Void>> logout(
            @RequestHeader("Authorization") String authorization,
            HttpServletResponse response) {

        String token = authorization.replace("Bearer ", "");
        authService.logout(token);

        // Refresh Token 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @PostMapping("/refresh")
    public ResponseEntity<CustomResponse<TokenResponse>> refreshToken(
            @CookieValue("refreshToken") String refreshToken) {

        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(CustomResponse.of(tokenResponse));
    }


}
