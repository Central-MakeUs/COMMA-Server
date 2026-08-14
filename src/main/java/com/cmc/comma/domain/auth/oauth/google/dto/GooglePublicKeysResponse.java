package com.cmc.comma.domain.auth.oauth.google.dto;

import java.util.List;

/** https://www.googleapis.com/oauth2/v3/certs 응답 — id_token 서명 검증에 쓰는 구글의 공개키 목록(JWKS). */
public record GooglePublicKeysResponse(List<GooglePublicKey> keys) {

    public record GooglePublicKey(
            String kty,
            String kid,
            String use,
            String alg,
            String n,
            String e
    ) {}
}
