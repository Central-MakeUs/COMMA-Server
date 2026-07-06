package com.cmc.comma.domain.auth.oauth;

import com.cmc.comma.domain.user.entity.Provider;

public record OAuthUserInfo(
        String providerId,
        String email,
        Provider provider
) {}
