package com.cmc.comma.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        boolean onboardingCompleted,
        String nickname
) {}