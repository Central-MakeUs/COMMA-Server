package com.cmc.comma.domain.auth.dto.request;

/** 프론트 SDK가 발급받은 토큰. 카카오=access_token, 구글/애플=id_token. */
public record LoginRequest(String token) {}
