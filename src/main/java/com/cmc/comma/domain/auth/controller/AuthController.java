package com.cmc.comma.domain.auth.controller;

import com.cmc.comma.domain.auth.dto.request.LoginRequest;
import com.cmc.comma.domain.auth.dto.response.TokenResponse;
import com.cmc.comma.domain.auth.service.AuthService;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/{provider}")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @PathVariable Provider provider,
            @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(provider, request.code(), request.redirectUri())));
    }
}