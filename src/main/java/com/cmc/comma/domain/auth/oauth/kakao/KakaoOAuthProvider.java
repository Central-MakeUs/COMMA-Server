package com.cmc.comma.domain.auth.oauth.kakao;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.kakao.dto.KakaoUserInfoResponse;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** 프론트 카카오 SDK가 발급한 access_token을 그대로 받아 유저 정보만 조회한다(code 교환 없음). */
@Slf4j
@Component
public class KakaoOAuthProvider implements OAuthProvider {

    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
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

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }
}
