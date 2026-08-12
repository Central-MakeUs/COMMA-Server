package com.cmc.comma.domain.auth.oauth.apple.dto;

import java.util.List;

/** https://appleid.apple.com/auth/keys 응답 — id_token 서명 검증에 쓰는 Apple의 공개키 목록(JWKS). */
public record ApplePublicKeysResponse(List<ApplePublicKey> keys) {

    public record ApplePublicKey(
            String kty,
            String kid,
            String use,
            String alg,
            String n,
            String e
    ) {}
}
