package com.cmc.comma.support;

import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.domain.user.entity.Role;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.auth.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 베이스. 진짜 MySQL/Redis 컨테이너 위에서 전체 스프링 컨텍스트를 띄운다
 * (Flyway 마이그레이션도 실제로 실행됨 → 운영과 동일한 스키마로 검증).
 * 컨테이너는 JVM당 한 번만 뜨고(싱글턴) 컨텍스트 캐시로 재사용된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected UserRepository userRepository;

    /** 온보딩 완료된 테스트 유저 생성. */
    protected User createUser(String nickname) {
        return userRepository.save(User.builder()
                .email(nickname + "@test.com")
                .nickname(nickname)
                .provider(Provider.KAKAO)
                .providerId("pid-" + nickname)
                .role(Role.USER)
                .onboardingCompleted(true)
                .build());
    }

    /** 해당 유저의 access 토큰을 Bearer 헤더 값으로 반환. */
    protected String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(userId);
    }
}
