package com.cmc.comma.domain.mypage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class MyReportIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("토큰 없이 마이 리포트 조회 시 인증 실패(4xx)")
    void report_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/mypage/report"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("활동이 없는 유저의 리포트: 순위는 비어있고 무드 비율은 A/B/C 3개가 모두 0")
    void report_newUser_emptyRankingAndThreeMoods() throws Exception {
        User user = createUser("리포트유저");

        mockMvc.perform(get("/api/mypage/report")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityRanking").isArray())
                .andExpect(jsonPath("$.data.activityRanking.length()").value(0))
                .andExpect(jsonPath("$.data.moodRatio.length()").value(3))
                .andExpect(jsonPath("$.data.moodRatio[0].count").value(0))
                .andExpect(jsonPath("$.data.moodRatio[0].ratio").value(0.0));
    }
}
