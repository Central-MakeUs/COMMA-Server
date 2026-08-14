package com.cmc.comma.domain.auth.oauth.apple;

import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.oauth.apple.dto.ApplePublicKeysResponse;
import com.cmc.comma.domain.auth.oauth.apple.dto.ApplePublicKeysResponse.ApplePublicKey;
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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 프론트 애플 SDK(AppleID.auth.signIn)가 발급한 id_token을 그대로 받아 서명만 검증한다(code 교환 없음). */
@Slf4j
@Component
public class AppleOAuthProvider implements OAuthProvider {

    private static final String KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String ISSUER = "https://appleid.apple.com";

    @Value("${apple.client-id}")
    private String clientId;

    private final RestClient restClient = RestClient.create();

    // Apple 공개키(JWKS) 캐시. kid로 못 찾으면 재조회한다(키 로테이션 대응).
    private volatile Map<String, PublicKey> publicKeys = Map.of();

    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        Claims claims = verifyIdToken(idToken);
        String sub = claims.getSubject();
        String email = claims.get("email", String.class);
        return new OAuthUserInfo(sub, email, Provider.APPLE);
    }

    @Override
    public Provider getProvider() {
        return Provider.APPLE;
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
