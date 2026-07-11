package com.cmc.comma.domain.auth.service;

import com.cmc.comma.domain.auth.dto.response.TokenResponse;
import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
import com.cmc.comma.domain.auth.repository.RefreshTokenRepository;
import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.auth.jwt.JwtTokenProvider;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final Map<Provider, OAuthProvider> oauthProviders;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(List<OAuthProvider> providers, UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository) {
        this.oauthProviders = providers.stream()
                .collect(Collectors.toMap(OAuthProvider::getProvider, p -> p));
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public TokenResponse login(Provider provider, String code, String redirectUri) {
        OAuthProvider oauthProvider = oauthProviders.get(provider);
        if (oauthProvider == null) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }

        OAuthUserInfo userInfo = oauthProvider.getUserInfo(code, redirectUri);
        User user = findOrCreateUser(userInfo);

        return issueTokens(user);
    }

    /**
     * 리프레시 토큰 재발급. 로테이션 + 재사용 탐지를 수행한다.
     * - 서명/만료가 유효하지 않으면 거부
     * - 저장된 토큰이 없으면(만료/로그아웃) 거부
     * - 저장된 토큰과 다르면 = 이미 회전된 옛 토큰의 재사용 → 세션 전체 폐기 후 거부
     * - 정상이면 access/refresh 모두 새로 발급하고 저장 토큰을 교체
     */
    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CommaException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String stored = refreshTokenRepository.find(userId);
        if (stored == null) {
            throw new CommaException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!stored.equals(refreshToken)) {
            // 폐기됐어야 할 토큰이 다시 들어옴 → 탈취 의심, 세션 전체 무효화
            refreshTokenRepository.delete(userId);
            throw new CommaException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        return new TokenResponse(
                accessToken,
                refreshToken,
                user.isOnboardingCompleted(),
                // 온보딩 전에는 임시 placeholder 닉네임이므로 노출하지 않는다
                user.isOnboardingCompleted() ? user.getNickname() : null
        );
    }

    private User findOrCreateUser(OAuthUserInfo userInfo) {
        return userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(userInfo.email())
                                .provider(userInfo.provider())
                                .providerId(userInfo.providerId())
                                // 온보딩 전까지 쓸 임시 유니크 placeholder (유저에게 노출되지 않음)
                                .nickname("temp_" + UUID.randomUUID())
                                .build()
                ));
    }
}