package com.cmc.comma.domain.auth.dto.request;

public record LoginRequest(String code, String redirectUri) {}