package com.cmc.comma.domain.auth.oauth.apple;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.apple.dto.AppleTokenResponse;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AppleOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String AUDIENCE = "https://appleid.apple.com";
    private static final long CLIENT_SECRET_EXPIRATION = 1000L * 60 * 30; // 30분

    @Value("${apple.client-id}")
    private String clientId;

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${apple.private-key}")
    private String privateKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        AppleTokenResponse token = getToken(code, redirectUri);
        return parseIdToken(token.idToken());
    }

    @Override
    public Provider getProvider() {
        return Provider.APPLE;
    }

    private AppleTokenResponse getToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", generateClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        return restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(AppleTokenResponse.class);
    }

    private String generateClientSecret() {
        Date now = new Date();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + CLIENT_SECRET_EXPIRATION))
                .audience().add(AUDIENCE).and()
                .subject(clientId)
                .signWith(loadPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey loadPrivateKey() {
        try {
            String key = privateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception e) {
            log.error("[APPLE] 개인키 로딩 실패", e);
            throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private OAuthUserInfo parseIdToken(String idToken) {
        try {
            String payload = idToken.split("\\.")[1];
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            JsonNode claims = objectMapper.readTree(decoded);
            String sub = claims.get("sub").asText();
            String email = claims.hasNonNull("email") ? claims.get("email").asText() : null;
            return new OAuthUserInfo(sub, email, Provider.APPLE);
        } catch (Exception e) {
            log.error("[APPLE] id_token 파싱 실패", e);
            throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
