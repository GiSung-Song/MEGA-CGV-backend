package com.cgv.mega.auth;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.auth.dto.LoginRequest;
import com.cgv.mega.common.security.JwtTokenProvider;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TestDataFactory testDataFactory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
    }

    private User user;

    @BeforeEach
    void setUp() {
        user = testDataFactory.createUser("user", "a@b.com", "01012341234");
    }

    @Test
    void 로그인_테스트() throws Exception {
        LoginRequest request = new LoginRequest(user.getEmail(), "rawPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andDo(document("auth-login",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.accessToken").description("JWT Access Token"),
                                fieldWithPath("data.refreshToken").description("Refresh Token").optional()
                        )
                ))
                .andDo(print());
    }

    @Test
    void 로그아웃_테스트() throws Exception {
        JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());
        String accessToken = jwtTokenProvider.generateAccessToken(jwtPayloadDto);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andDo(document("auth-logout",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ))
                .andDo(print());
    }

    @Test
    void 토큰_재발급_테스트() throws Exception {
        JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtPayloadDto);

        redisTemplate.opsForValue().set("refresh:" + user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andDo(document("auth-refresh",
                        requestCookies(
                                cookieWithName("refreshToken").description("JWT Refresh Token")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.accessToken").description("새로운 JWT Access Token"),
                                fieldWithPath("data.refreshToken").description("JWT Refresh Token").optional()
                        )
                ))
                .andDo(print());
    }
}
