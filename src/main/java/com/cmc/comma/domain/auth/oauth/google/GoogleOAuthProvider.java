package com.cmc.comma.domain.auth.oauth.google;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.google.dto.GooglePublicKeysResponse;
import com.cmc.comma.domain.auth.oauth.google.dto.GooglePublicKeysResponse.GooglePublicKey;
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
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 프론트 구글 SDK(Google Identity Services)가 발급한 id_token을 그대로 받아 서명만 검증한다(code 교환 없음). */
@Slf4j
@Component
public class GoogleOAuthProvider implements OAuthProvider {

    private static final String KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    // 구글은 iss가 두 형태 중 하나로 온다 — 둘 다 유효하다고 문서에 명시돼있음.
    private static final Set<String> VALID_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    @Value("${google.client-id}")
    private String clientId;

    private final RestClient restClient = RestClient.create();

    // 구글 공개키(JWKS) 캐시. kid로 못 찾으면 재조회한다(키 로테이션 대응).
    private volatile Map<String, PublicKey> publicKeys = Map.of();

    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        Claims claims = verifyIdToken(idToken);
        String sub = claims.getSubject();
        String email = claims.get("email", String.class);
        return new OAuthUserInfo(sub, email, Provider.GOOGLE);
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    /**
     * id_token 서명을 구글 공개키로 검증하고(위조 방지), iss/aud/exp까지 확인한 뒤 claims를 반환한다.
     * 서명 검증 없이 payload만 디코드하면 위조된 id_token도 그대로 통과하므로 반드시 거쳐야 하는 단계.
     */
    private Claims verifyIdToken(String idToken) {
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(this::resolveSigningKey)
                    .requireAudience(clientId)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
            if (!VALID_ISSUERS.contains(claims.getIssuer())) {
                throw new CommaException(ErrorCode.INVALID_TOKEN);
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("[GOOGLE] id_token 검증 실패", e);
            throw new CommaException(ErrorCode.INVALID_TOKEN);
        }
    }

    /** JWT 헤더의 kid로 서명에 쓰인 구글 공개키를 찾는다. 캐시에 없으면 JWKS를 다시 받아온다. */
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

    /** 구글 JWKS를 새로 받아와 캐시를 통째로 교체한다(키 로테이션 대응). */
    private synchronized Map<String, PublicKey> refreshPublicKeys() {
        GooglePublicKeysResponse response = restClient.get()
                .uri(KEYS_URL)
                .retrieve()
                .body(GooglePublicKeysResponse.class);
        Map<String, PublicKey> resolved = response.keys().stream()
                .collect(Collectors.toMap(GooglePublicKey::kid, this::toPublicKey));
        this.publicKeys = resolved;
        return resolved;
    }

    private PublicKey toPublicKey(GooglePublicKey jwk) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(jwk.n());
            byte[] eBytes = Base64.getUrlDecoder().decode(jwk.e());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("[GOOGLE] 공개키 파싱 실패 kid={}", jwk.kid(), e);
            throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
