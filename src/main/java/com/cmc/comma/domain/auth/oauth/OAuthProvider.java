package com.cmc.comma.domain.auth.oauth;

import com.cmc.comma.domain.user.entity.Provider;

public interface OAuthProvider {

    OAuthUserInfo getUserInfo(String code, String redirectUri);

    Provider getProvider();
}