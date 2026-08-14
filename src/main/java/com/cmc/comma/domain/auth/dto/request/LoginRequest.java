package com.cmc.comma.domain.auth.dto.request;

/** 기존 방식(웹 리다이렉트): authorization code + 그때 쓴 redirect_uri. */
public record LoginRequest(String code, String redirectUri) {}
