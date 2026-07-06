package com.cmc.comma.domain.auth.oauth.google.dto;

public record GoogleUserInfoResponse(
        String sub,
        String email
) {}