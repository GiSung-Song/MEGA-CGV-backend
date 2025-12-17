package com.cgv.mega.auth;

import com.cgv.mega.auth.dto.LoginRequest;
import com.cgv.mega.auth.dto.TokenResponse;
import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthService authService;

    @Nested
    class 로그인_테스 {

        @Test
        void 로그인_성공() throws Exception {
            LoginRequest request = new LoginRequest("a@b.com", "rawPassword");
            TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");

            given(authService.login(any(LoginRequest.class))).willReturn(tokenResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(print())
                    .andExpect(cookie().value("refreshToken", tokenResponse.refreshToken()));
        }

        @Test
        void 필수값_누락_실패_400반환() throws Exception {
            LoginRequest request = new LoginRequest("a@b.com", null);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 서비스에서_401예외_던질_경우_실패() throws Exception {
            LoginRequest request = new LoginRequest("a@b.com", "rawPassword");
            given(authService.login(any(LoginRequest.class))).willThrow(new CustomException(ErrorCode.LOGIN_FAIL));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    class 로그아웃 {

        @Test
        void 로그아웃_정상() throws Exception {
            String accessToken = "Bearer AccessToken";
            willDoNothing().given(authService).logout(anyString());

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", accessToken))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().maxAge("refreshToken", 0))
                    .andDo(print());
        }

        @Test
        void 헤더_누락_400반환() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 토큰_재발급 {

        @Test
        void 토큰_재발급_성공() throws Exception {
            TokenResponse tokenResponse = new TokenResponse("access-token", null);

            given(authService.refreshToken(anyString())).willReturn(tokenResponse);

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refreshToken", "refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andDo(print());
        }

        @Test
        void 쿠키_누락시_400반환() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 서비스에서_401예외_던질_경우_실패() throws Exception {
            given(authService.refreshToken(anyString())).willThrow(new CustomException(ErrorCode.JWT_TOKEN_INVALID));

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new Cookie("refreshToken", "refresh-token")))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}
