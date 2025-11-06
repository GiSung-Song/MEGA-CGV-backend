package com.cgv.mega.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ChangePasswordRequest(
        @Length(min = 8, message = "현재 비밀번호는 최소 8자입니다.")
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @Length(min = 8, message = "새 비밀번호는 최소 8자입니다.")
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword
) {
}