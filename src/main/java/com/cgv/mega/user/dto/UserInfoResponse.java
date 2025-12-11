package com.cgv.mega.user.dto;

import com.cgv.mega.user.entity.User;

public record UserInfoResponse(
        String name,
        String email,
        String phoneNumber
) {
    public static UserInfoResponse toDto(User user) {
        return new UserInfoResponse(user.getName(), user.getEmail(), user.getPhoneNumber());
    }
}
