package com.cmc.comma.domain.auth.service;

import com.cmc.comma.domain.auth.dto.response.TokenResponse;
import com.cmc.comma.domain.auth.oauth.OAuthProvider;
import com.cmc.comma.domain.auth.oauth.OAuthUserInfo;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final Map<Provider, OAuthProvider> oauthProviders;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(List<OAuthProvider> providers, UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.oauthProviders = providers.stream()
                .collect(Collectors.toMap(OAuthProvider::getProvider, p -> p));
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public TokenResponse login(Provider provider, String code, String redirectUri) {
        OAuthProvider oauthProvider = oauthProviders.get(provider);
        if (oauthProvider == null) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }

        OAuthUserInfo userInfo = oauthProvider.getUserInfo(code, redirectUri);
        User user = findOrCreateUser(userInfo);

        return new TokenResponse(
                jwtTokenProvider.generateAccessToken(user.getId()),
                jwtTokenProvider.generateRefreshToken(),
                user.isOnboardingCompleted()
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