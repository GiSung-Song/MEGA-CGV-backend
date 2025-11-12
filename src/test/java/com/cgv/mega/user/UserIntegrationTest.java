package com.cgv.mega.user;

import com.cgv.mega.auth.dto.JwtPayloadDto;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.security.JwtTokenProvider;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.user.dto.ChangePasswordRequest;
import com.cgv.mega.user.dto.ChangePhoneNumberRequest;
import com.cgv.mega.user.dto.RegisterUserRequest;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import com.cgv.mega.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    private User user;
    private String accessToken;

    @BeforeEach
    void setLogin() {
        user = testDataFactory.createUser("user", "a@b.com", "01012345678");
        JwtPayloadDto jwtPayloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

        accessToken = "Bearer " + jwtTokenProvider.generateAccessToken(jwtPayloadDto);
    }

    @Test
    void 회원가입_테스트() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest("tester", "b@c.com", "rawPassword", "01013572468");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("user-register",
                        requestFields(
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("password").description("비밀번호"),
                                fieldWithPath("phoneNumber").description("휴대폰 번호")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ))
                .andDo(print());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        assertThat(user.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(user.getName()).isEqualTo(request.name());
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void 회원_탈퇴_테스트() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andDo(document("user-delete",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ))
                .andDo(print());

        User findUser = userRepository.findByEmail(user.getEmail())
                .orElse(null);

        assertThat(findUser).isNull();
    }

    @Test
    void 비밀번호_변경_테스트() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("rawPassword", "newPassword");

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("user-change-password",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
                        requestFields(
                                fieldWithPath("currentPassword").description("현재 비밀번호"),
                                fieldWithPath("newPassword").description("새로운 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ))
                .andDo(print());

        User findUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow();

        assertThat(passwordEncoder.matches(request.newPassword(), findUser.getPassword())).isTrue();
    }

    @Test
    void 휴대폰_번호_변경_테스트() throws Exception {
        ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01098765432");

        mockMvc.perform(patch("/api/users/me/phone-number")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("user-change-phone-number",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
                        requestFields(
                                fieldWithPath("phoneNumber").description("휴대폰 번호")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지")
                        )
                ))
                .andDo(print());

        User findUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow();

        assertThat(findUser.getPhoneNumber()).isEqualTo(request.phoneNumber());
    }

    @Test
    void 회원정보_조회_테스트() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(user.getName()))
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.phoneNumber").value(user.getPhoneNumber()))
                .andDo(document("user-info",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.email").description("이메일"),
                                fieldWithPath("data.phoneNumber").description("휴대폰 번호")
                        )
                ))
                .andDo(print());
    }
}
