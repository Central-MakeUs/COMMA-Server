package com.cmc.comma.domain.auth.oauth.google;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.google.dto.GoogleTokenResponse;
import com.cmc.comma.domain.auth.oauth.google.dto.GoogleUserInfoResponse;
import com.cmc.comma.domain.user.entity.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GoogleOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    @Override
    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        String accessToken = getAccessToken(code, redirectUri);
        return fetchUserInfo(accessToken);
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    private String getAccessToken(String code, String redirectUri) {
        log.info("[GOOGLE] code = [{}], redirect_uri = [{}]", code, redirectUri);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        GoogleTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(GoogleTokenResponse.class);
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        GoogleUserInfoResponse response = restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfoResponse.class);
        return new OAuthUserInfo(response.sub(), response.email(), Provider.GOOGLE);
    }
}