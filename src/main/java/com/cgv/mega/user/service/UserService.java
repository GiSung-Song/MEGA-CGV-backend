package com.cgv.mega.user.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.user.dto.ChangePasswordRequest;
import com.cgv.mega.user.dto.ChangePhoneNumberRequest;
import com.cgv.mega.user.dto.RegisterUserRequest;
import com.cgv.mega.user.dto.UserInfoResponse;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public void registerUser(RegisterUserRequest request) {
        // 사용자 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 사용자 휴대폰 번호 중복 체크
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        User user = User.createUser(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.phoneNumber()
        );

        // 저장
        userRepository.save(user);
    }

    // 회원탈퇴
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId).ifPresent(userRepository::delete);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INCORRECT_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    // 휴대폰 번호 변경
    @Transactional
    public void changePhoneNumber(Long userId, ChangePhoneNumberRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getPhoneNumber().equals(request.phoneNumber())) {
            return;
        }

        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        user.changePhoneNumber(request.phoneNumber());
    }

    // 회원 조회
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserInfoResponse.toDto(user);
    }
}
