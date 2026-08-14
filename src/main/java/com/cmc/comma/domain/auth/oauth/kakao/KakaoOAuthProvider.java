package com.cmc.comma.domain.auth.oauth.kakao;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.kakao.dto.KakaoTokenResponse;
import com.cmc.comma.domain.auth.oauth.kakao.dto.KakaoUserInfoResponse;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 프론트 마이그레이션 기간 동안 두 방식을 같이 지원한다.
 * - 기존: authorization code를 서버가 이 토큰 엔드포인트와 교환
 * - SDK: 프론트 카카오 SDK가 이미 발급한 access_token을 그대로 받음
 * 프론트가 SDK 전환을 마치면 code 교환 경로({@link #getUserInfo(String, String)}, {@link #getAccessToken})는 삭제한다.
 */
@Slf4j
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
    public OAuthUserInfo getUserInfoFromToken(String accessToken) {
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
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
            return new OAuthUserInfo(String.valueOf(response.id()), response.kakaoAccount().email(), Provider.KAKAO);
        } catch (RestClientResponseException e) {
            // 카카오는 별도 서명 검증이 없어 이 호출 실패 자체가 "토큰이 무효함"의 근거다(만료/폐기/위조).
            log.warn("[KAKAO] 유저 정보 조회 실패 status={}", e.getStatusCode());
            throw new CommaException(ErrorCode.INVALID_TOKEN);
        }
    }
}
