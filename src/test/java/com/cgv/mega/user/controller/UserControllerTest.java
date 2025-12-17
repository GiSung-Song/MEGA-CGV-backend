package com.cgv.mega.user.controller;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.user.dto.ChangePasswordRequest;
import com.cgv.mega.user.dto.ChangePhoneNumberRequest;
import com.cgv.mega.user.dto.RegisterUserRequest;
import com.cgv.mega.user.dto.UserInfoResponse;
import com.cgv.mega.user.service.UserService;
import com.cgv.mega.util.CustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    class 회원가입 {

        @Test
        void 회원가입_성공_201반환() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest("user", "a@b.com", "rawPassword", "01012341234");
            doNothing().when(userService).registerUser(request);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(print());
        }

        @Test
        void 필수값_누락_400반환() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest("user", null, "rawPassword", "01012341234");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 서비스에서_409반환_실패() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest("user", "a@b.com", "rawPassword", "01012341234");
            doThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL)).when(userService).registerUser(request);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andDo(print());
        }
    }

    @Nested
    class 회원_탈퇴 {

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 회원탈퇴_성공_200반환() throws Exception {
            doNothing().when(userService).deleteUser(1L);

            mockMvc.perform(delete("/api/users/me"))
                    .andExpect(status().isOk())
                    .andDo(print());
        }
    }

    @Nested
    class 비밀번호_변경 {

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 비밀번호_변경_성공_200반환() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "newPassword");

            doNothing().when(userService).changePassword(anyLong(), any(ChangePasswordRequest.class));

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 필수값_누락_400반환() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(null, "newPassword");

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 서비스에서_400반환_실패() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "newPassword");

            doThrow(new CustomException(ErrorCode.INCORRECT_PASSWORD)).when(userService).changePassword(anyLong(), any(ChangePasswordRequest.class));

            mockMvc.perform(patch("/api/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    class 휴대폰_번호_변경 {

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 휴대폰_번호_변경_성공_200반환() throws Exception {
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01013572468");

            doNothing().when(userService).changePhoneNumber(anyLong(), any(ChangePhoneNumberRequest.class));

            mockMvc.perform(patch("/api/users/me/phone-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 필수값_누락_400반환() throws Exception {
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest(null);

            mockMvc.perform(patch("/api/users/me/phone-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 서비스에서_409반환_실패() throws Exception {
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01013572468");

            doThrow(new CustomException(ErrorCode.DUPLICATE_PHONE_NUMBER))
                    .when(userService).changePhoneNumber(anyLong(), any(ChangePhoneNumberRequest.class));

            mockMvc.perform(patch("/api/users/me/phone-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andDo(print());
        }
    }

    @Nested
    class 사용자_정보_조회 {

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 사용자_정보_조회_200반환() throws Exception {
            UserInfoResponse response = new UserInfoResponse("user", "a@b.com", "01024243636");

            given(userService.getUserInfo(1L)).willReturn(response);

            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value(response.name()))
                    .andExpect(jsonPath("$.data.email").value(response.email()))
                    .andExpect(jsonPath("$.data.phoneNumber").value(response.phoneNumber()))
                    .andDo(print());
        }

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 서비스에서_404반환_실패() throws Exception {
            doThrow(new CustomException(ErrorCode.USER_NOT_FOUND)).when(userService).getUserInfo(1L);

            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isNotFound())
                    .andDo(print());
        }
    }
}