package com.cgv.mega.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record RegisterUserRequest(

        @Length(max = 50, message = "이름은 최대 50자입니다.")
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @Length(max = 50, message = "이메일은 최대 50자입니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식으로 작성해주세요.")
        String email,

        @Length(min = 8, message = "비밀번호는 최소 8자입니다.")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Pattern(regexp = "^[0-9]{10,20}$", message = "휴대폰 번호는 숫자 10~20자입니다.")
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phoneNumber
) {
}
