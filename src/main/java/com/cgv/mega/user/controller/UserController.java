package com.cgv.mega.user.controller;

import com.cgv.mega.common.response.CustomResponse;
import com.cgv.mega.common.security.CustomUserDetails;
import com.cgv.mega.user.dto.ChangePasswordRequest;
import com.cgv.mega.user.dto.ChangePhoneNumberRequest;
import com.cgv.mega.user.dto.RegisterUserRequest;
import com.cgv.mega.user.dto.UserInfoResponse;
import com.cgv.mega.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<CustomResponse<Void>> registerUser(@RequestBody @Valid RegisterUserRequest request) {
        userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.of(HttpStatus.CREATED));
    }

    @DeleteMapping("/me")
    public ResponseEntity<CustomResponse<Void>> deleteUser(@AuthenticationPrincipal CustomUserDetails details) {
        userService.deleteUser(details.id());

        return ResponseEntity.ok(CustomResponse.of());
    }

    @PatchMapping("/me/password")
    public ResponseEntity<CustomResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        userService.changePassword(details.id(), request);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @PatchMapping("/me/phone-number")
    public ResponseEntity<CustomResponse<Void>> changePhoneNumber(
            @RequestBody @Valid ChangePhoneNumberRequest request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        userService.changePhoneNumber(details.id(), request);

        return ResponseEntity.ok(CustomResponse.of());
    }

    @GetMapping("/me")
    public ResponseEntity<CustomResponse<UserInfoResponse>> getUserInfo(@AuthenticationPrincipal CustomUserDetails details) {
        UserInfoResponse userInfo = userService.getUserInfo(details.id());

        return ResponseEntity.ok(CustomResponse.of(userInfo));
    }
}
