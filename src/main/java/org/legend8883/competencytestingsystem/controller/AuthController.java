package org.legend8883.competencytestingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.LoginRequest;
import org.legend8883.competencytestingsystem.dto.request.RegisterRequest;
import org.legend8883.competencytestingsystem.dto.response.ApiResponse;
import org.legend8883.competencytestingsystem.dto.response.AuthResponse;
import org.legend8883.competencytestingsystem.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Регистрация успешна", response));
    }

    @PostMapping("/register-hr")
    public ResponseEntity<ApiResponse<AuthResponse>> registerHr(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerHr(request);
        return ResponseEntity.ok(ApiResponse.success("HR зарегистрирован", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Вход выполнен", response));
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("Auth endpoint works!", "OK"));
    }
}
