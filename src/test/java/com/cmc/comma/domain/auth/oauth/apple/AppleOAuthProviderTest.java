package com.cmc.comma.domain.auth.oauth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * id_token 서명 검증 로직만 떼어서 단위 테스트. 네트워크(토큰 교환, JWKS 조회) 없이
 * publicKeys 캐시를 직접 주입해서 "서명이 안 맞으면 거부되는지"를 확인한다.
 */
class AppleOAuthProviderTest {

    private static final String CLIENT_ID = "com.cmc.comma.test";
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String KID = "test-kid";

    private AppleOAuthProvider provider;
    private KeyPair appleKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        provider = new AppleOAuthProvider();
        ReflectionTestUtils.setField(provider, "clientId", CLIENT_ID);

        appleKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        ReflectionTestUtils.setField(provider, "publicKeys", Map.of(KID, appleKeyPair.getPublic()));
    }

    @Test
    @DisplayName("Apple 개인키로 서명되고 iss/aud가 맞는 id_token은 검증을 통과한다")
    void verifyIdToken_validSignature_succeeds() {
        String idToken = signToken(appleKeyPair.getPrivate(), ISSUER, CLIENT_ID);

        Claims claims = ReflectionTestUtils.invokeMethod(provider, "verifyIdToken", idToken);

        assertThat(claims.getSubject()).isEqualTo("apple-user-1");
    }

    @Test
    @DisplayName("다른 키로 서명된(위조된) id_token은 거부한다")
    void verifyIdToken_forgedSignature_rejected() throws Exception {
        KeyPair attackerKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        // 공격자가 자기 키로 서명하되 헤더의 kid는 진짜인 척 그대로 사용
        String forged = signTokenWithKid(attackerKeyPair.getPrivate(), ISSUER, CLIENT_ID, KID);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(provider, "verifyIdToken", forged))
                .isInstanceOf(CommaException.class)
                .extracting(e -> ((CommaException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("발급자(iss)가 다른 id_token은 거부한다")
    void verifyIdToken_wrongIssuer_rejected() {
        String idToken = signToken(appleKeyPair.getPrivate(), "https://evil.example.com", CLIENT_ID);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(provider, "verifyIdToken", idToken))
                .isInstanceOf(CommaException.class);
    }

    @Test
    @DisplayName("대상(aud)이 우리 clientId가 아닌 id_token은 거부한다")
    void verifyIdToken_wrongAudience_rejected() {
        String idToken = signToken(appleKeyPair.getPrivate(), ISSUER, "other-app-client-id");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(provider, "verifyIdToken", idToken))
                .isInstanceOf(CommaException.class);
    }

    private String signToken(PrivateKey privateKey, String issuer, String audience) {
        return signTokenWithKid(privateKey, issuer, audience, KID);
    }

    private String signTokenWithKid(PrivateKey privateKey, String issuer, String audience, String kid) {
        Date now = new Date();
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject("apple-user-1")
                .claim("email", "apple-user@example.com")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
