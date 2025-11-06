package com.cgv.mega.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePhoneNumberRequest(
        @Pattern(regexp = "^[0-9]{10,20}$", message = "휴대폰 번호는 숫자 10~20자입니다.")
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phoneNumber
) {
}
