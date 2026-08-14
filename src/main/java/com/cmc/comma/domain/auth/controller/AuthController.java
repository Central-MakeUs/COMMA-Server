package com.cmc.comma.domain.auth.controller;

import com.cmc.comma.domain.auth.dto.request.LoginRequest;
import com.cmc.comma.domain.auth.dto.request.ReissueRequest;
import com.cmc.comma.domain.auth.dto.request.SdkLoginRequest;
import com.cmc.comma.domain.auth.dto.response.TokenResponse;
import com.cmc.comma.domain.auth.service.AuthService;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.global.response.ApiResponse;
import com.cmc.comma.global.util.SecurityUtil;
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

    /** 기존 방식(웹 리다이렉트). 프론트 SDK 마이그레이션이 끝나면 삭제한다. */
    @PostMapping("/login/{provider}")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @PathVariable Provider provider,
            @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(provider, request.code(), request.redirectUri())));
    }

    /** SDK 방식. 프론트 SDK 마이그레이션이 끝나면 위 {@link #login}을 대체한다. */
    @PostMapping("/login/sdk/{provider}")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithSdk(
            @PathVariable Provider provider,
            @RequestBody SdkLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithSdkToken(provider, request.token())));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@RequestBody ReissueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout(SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}