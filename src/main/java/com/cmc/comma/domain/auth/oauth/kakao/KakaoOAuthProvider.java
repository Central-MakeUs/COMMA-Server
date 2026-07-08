package com.cmc.comma.domain.auth.oauth.kakao;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.kakao.dto.KakaoTokenResponse;
import com.cmc.comma.domain.auth.oauth.kakao.dto.KakaoUserInfoResponse;
import com.cmc.comma.domain.user.entity.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${kakao.client-id}")
    private String clientId;

    private final RestClient restClient = RestClient.create();

    @Override
    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        String accessToken = getAccessToken(code, redirectUri);
        return fetchUserInfo(accessToken);
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    private String getAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(KakaoTokenResponse.class);
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        KakaoUserInfoResponse response = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);
        return new OAuthUserInfo(String.valueOf(response.id()), response.kakaoAccount().email(), Provider.KAKAO);
    }
}