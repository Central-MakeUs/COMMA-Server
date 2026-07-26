package com.cmc.comma;

import com.cmc.comma.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

/**
 * 전체 스프링 컨텍스트가 실제 MySQL/Redis(Testcontainers) 위에서 뜨는지 검증.
 * Flyway 마이그레이션 실행 + 모든 빈 와이어링 + JPA validate가 여기서 함께 검증된다.
 */
class CommaApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {
    }
}
