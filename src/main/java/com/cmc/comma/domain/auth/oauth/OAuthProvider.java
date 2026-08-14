package com.cmc.comma.domain.auth.oauth;

import com.cmc.comma.domain.user.entity.Provider;

public interface OAuthProvider {

    /** 기존 방식: 프론트가 리다이렉트로 받은 authorization code를 서버가 provider와 교환해서 유저 정보를 조회한다. */
    OAuthUserInfo getUserInfo(String code, String redirectUri);

    /** SDK 방식: 프론트 SDK가 이미 발급받은 토큰(카카오=access_token, 구글/애플=id_token)으로 바로 유저 정보를 조회한다. */
    OAuthUserInfo getUserInfoFromToken(String token);

    Provider getProvider();
}
