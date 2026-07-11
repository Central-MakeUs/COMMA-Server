package com.cmc.comma.domain.auth.repository;

import com.cmc.comma.global.auth.jwt.JwtTokenProvider;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 리프레시 토큰을 Redis에 저장/조회/삭제한다.
 * 키: refresh_token:{userId}, 값: 현재 유효한 리프레시 토큰, TTL: 리프레시 토큰 만료시간.
 * 유저당 1개만 유지하므로 로그인/재발급 시 이전 토큰은 자연스럽게 덮어써진다(회전).
 */
@Repository
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpiration());
    }

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    public String find(Long userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
