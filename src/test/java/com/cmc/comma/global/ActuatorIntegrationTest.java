package com.cmc.comma.global;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActuatorIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("health는 인증 없이 공개되고 UP을 반환")
    void health_isPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("prometheus 지표는 앱 내부에서 접근 가능(내부 스크레이핑용, 외부 차단은 nginx 담당)")
    void prometheus_isScrapableInternally() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }
}
