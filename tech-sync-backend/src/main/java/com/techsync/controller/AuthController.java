package com.techsync.controller;

import com.techsync.dto.ApiResponse;
import com.techsync.dto.AuthResponse;
import com.techsync.dto.FindIdRequest;
import com.techsync.dto.FindIdResponse;
import com.techsync.dto.LoginRequest;
import com.techsync.dto.ResetPasswordRequest;
import com.techsync.dto.SignupRequest;
import com.techsync.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Refresh-Token") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@Valid @RequestBody FindIdRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.findEmailsByName(request.name())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.name(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
