package com.cgv.mega.user.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.enums.Role;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.user.dto.ChangePasswordRequest;
import com.cgv.mega.user.dto.ChangePhoneNumberRequest;
import com.cgv.mega.user.dto.RegisterUserRequest;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    class 회원가입_테스트 {

        @Test
        void 회원가입_성공() {
            RegisterUserRequest request = new RegisterUserRequest("user", "a@b.com", "rawPassowrd", "01012341234");

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

            userService.registerUser(request);

            then(userRepository).should().save(argThat(user ->
                    user.getEmail().equals(request.email())
                            && user.getPassword().equals("encodedPassword")
                            && user.getPhoneNumber().equals(request.phoneNumber())
                            && user.getRole().equals(Role.USER)
            ));
        }

        @Test
        void 이메일_중복_409반환() {
            RegisterUserRequest request = new RegisterUserRequest("user", "a@b.com", "rawPassowrd", "01012341234");

            given(userRepository.existsByEmail(request.email())).willReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });

            then(userRepository).should(never()).save(any());
        }

        @Test
        void 핸드폰_번호_중복_409반환() {
            RegisterUserRequest request = new RegisterUserRequest("user", "a@b.com", "rawPassowrd", "01012341234");

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PHONE_NUMBER);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    class 회원탈퇴_테스트 {

        @Test
        void 회원_존재_탈퇴() {
            User mockUser = mock(User.class);
            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

            userService.deleteUser(1L);

            then(userRepository).should().delete(mockUser);
        }

        @Test
        void 회원_없음_탈퇴() {
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            userService.deleteUser(1L);

            then(userRepository).should(never()).delete(any());
        }
    }

    @Nested
    class 비밀번호_변경_테스트 {

        @Test
        void 비밀번호_변경_성공() {
            User user = User.createUser("user", "a@b.com", "encodedPassword", "01012341234");

            ChangePasswordRequest req = new ChangePasswordRequest("currentPassword", "newPassword");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(req.currentPassword(), "encodedPassword")).willReturn(true);
            given(passwordEncoder.encode(req.newPassword())).willReturn("encodedNewPassword");

            userService.changePassword(1L, req);

            assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
        }

        @Test
        void 회원없음_404반환() {
            ChangePasswordRequest req = new ChangePasswordRequest("currentPassword", "newPassword");

            given(userRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword(1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 현재_비밀번호_틀림_400반환() {
            User user = User.createUser("user", "a@b.com", "encodedPassword", "01012341234");

            ChangePasswordRequest req = new ChangePasswordRequest("currentPassword", "newPassword");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(req.currentPassword(), "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword(1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INCORRECT_PASSWORD);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }
    }

    @Nested
    class 휴대폰_번호_변경_테스트 {

        @Test
        void 휴대폰_번호_변경_성공() {
            User user = User.createUser("user", "a@b.com", "encodedPassword", "01012341234");

            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01013572468");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(false);

            userService.changePhoneNumber(1L, request);

            assertThat(user.getPhoneNumber()).isEqualTo(request.phoneNumber());
        }

        @Test
        void 휴대폰_번호_그대로_성공() {
            User user = User.createUser("user", "a@b.com", "encodedPassword", "01012341234");

            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01012341234");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.changePhoneNumber(1L, request);

            then(userRepository).should(never()).existsByPhoneNumber(request.phoneNumber());
        }

        @Test
        void 회원없음_404반환() {
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01012341234");

            given(userRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePhoneNumber(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void 중복된_휴대폰_번호_409반환() {
            User user = User.createUser("user", "a@b.com", "encodedPassword", "01012341234");
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("01013572468");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(true);

            assertThatThrownBy(() -> userService.changePhoneNumber(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException ex = (CustomException) exception;

                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PHONE_NUMBER);
                        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }
    }
}