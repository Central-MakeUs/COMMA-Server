package com.cmc.comma.domain.auth.oauth;

import com.cmc.comma.domain.user.entity.Provider;

public interface OAuthProvider {

    /** 프론트 SDK가 이미 발급받은 토큰(카카오=access_token, 구글/애플=id_token)으로 유저 정보를 조회한다. */
    OAuthUserInfo getUserInfo(String token);

    Provider getProvider();
}