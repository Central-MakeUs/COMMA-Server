package com.cmc.comma.domain.auth.oauth.apple;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.apple.dto.ApplePublicKeysResponse;
import com.cmc.comma.domain.auth.oauth.apple.dto.ApplePublicKeysResponse.ApplePublicKey;
import com.cmc.comma.domain.auth.oauth.apple.dto.AppleTokenResponse;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 프론트 마이그레이션 기간 동안 두 방식을 같이 지원한다.
 * - 기존: authorization code를 서버가 자체 서명한 client_secret으로 이 토큰 엔드포인트와 교환해 id_token을 받음
 * - SDK: 프론트 애플 SDK(AppleID.auth.signIn)가 이미 발급한 id_token을 그대로 받음
 * 서명 검증 로직(verifyIdToken 이하)은 두 경로가 공유한다. 프론트가 SDK 전환을 마치면 code 교환 경로
 * ({@link #getUserInfo(String, String)}, {@link #getToken}, {@link #generateClientSecret},
 * {@link #loadPrivateKey}, {@code team-id}/{@code key-id}/{@code private-key} 설정)는 삭제한다.
 */
@Slf4j
@Component
public class AppleOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String ISSUER = "https://appleid.apple.com";
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

    // Apple 공개키(JWKS) 캐시. kid로 못 찾으면 재조회한다(키 로테이션 대응).
    private volatile Map<String, PublicKey> publicKeys = Map.of();

    @Override
    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        AppleTokenResponse token = getToken(code, redirectUri);
        return parseIdToken(token.idToken());
    }

    @Override
    public OAuthUserInfo getUserInfoFromToken(String idToken) {
        return parseIdToken(idToken);
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
                .audience().add(ISSUER).and()
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
        Claims claims = verifyIdToken(idToken);
        String sub = claims.getSubject();
        String email = claims.get("email", String.class);
        return new OAuthUserInfo(sub, email, Provider.APPLE);
    }

    /**
     * id_token 서명을 Apple 공개키로 검증하고(위조 방지), iss/aud/exp까지 확인한 뒤 claims를 반환한다.
     * 서명 검증 없이 payload만 디코드하면 위조된 id_token도 그대로 통과하므로 반드시 거쳐야 하는 단계.
     */
    private Claims verifyIdToken(String idToken) {
        try {
            return Jwts.parser()
                    .keyLocator(this::resolveSigningKey)
                    .requireIssuer(ISSUER)
                    .requireAudience(clientId)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("[APPLE] id_token 검증 실패", e);
            throw new CommaException(ErrorCode.INVALID_TOKEN);
        }
    }

    /** JWT 헤더의 kid로 서명에 쓰인 Apple 공개키를 찾는다. 캐시에 없으면 JWKS를 다시 받아온다. */
    private PublicKey resolveSigningKey(Header header) {
        String kid = header instanceof ProtectedHeader protectedHeader ? protectedHeader.getKeyId() : null;
        if (kid == null) {
            throw new CommaException(ErrorCode.INVALID_TOKEN);
        }
        PublicKey key = publicKeys.get(kid);
        if (key == null) {
            key = refreshPublicKeys().get(kid);
        }
        if (key == null) {
            throw new CommaException(ErrorCode.INVALID_TOKEN);
        }
        return key;
    }

    /** Apple JWKS를 새로 받아와 캐시를 통째로 교체한다(키 로테이션 대응). */
    private synchronized Map<String, PublicKey> refreshPublicKeys() {
        ApplePublicKeysResponse response = restClient.get()
                .uri(KEYS_URL)
                .retrieve()
                .body(ApplePublicKeysResponse.class);
        Map<String, PublicKey> resolved = response.keys().stream()
                .collect(Collectors.toMap(ApplePublicKey::kid, this::toPublicKey));
        this.publicKeys = resolved;
        return resolved;
    }

    private PublicKey toPublicKey(ApplePublicKey jwk) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(jwk.n());
            byte[] eBytes = Base64.getUrlDecoder().decode(jwk.e());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("[APPLE] 공개키 파싱 실패 kid={}", jwk.kid(), e);
            throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
