package com.cmc.comma.domain.mypage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.entity.Relax;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class MyReportIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private RelaxRepository relaxRepository;

    private Relax seedRelax() {
        return relaxRepository.save(Relax.builder()
                .mood(Mood.A)
                .timeBudget(TimeBudget.X)
                .name("낮잠")
                .description("짧은 낮잠")
                .activeMessage("함께 자는 중")
                .imageKey("relaxes/seed.jpg")
                .build());
    }

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

    @Test
    @DisplayName("시작만 하고 완료(기록) 안 한 활동은 순위/무드 비율에 반영되지 않는다")
    void report_startedButNotCompleted_notCounted() throws Exception {
        User user = createUser("진행중유저");
        Relax relax = seedRelax();
        activityRepository.save(Activity.start(user.getId(), relax.getId())); // completedAt 없음

        mockMvc.perform(get("/api/mypage/report")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityRanking.length()").value(0))
                .andExpect(jsonPath("$.data.moodRatio[0].count").value(0));
    }

    @Test
    @DisplayName("완료(기록)된 활동만 순위/무드 비율에 반영된다")
    void report_completedActivity_counted() throws Exception {
        User user = createUser("완료유저");
        Relax relax = seedRelax();
        Activity activity = Activity.start(user.getId(), relax.getId());
        activity.complete();
        activityRepository.save(activity);

        mockMvc.perform(get("/api/mypage/report")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityRanking.length()").value(1))
                .andExpect(jsonPath("$.data.activityRanking[0].count").value(1))
                .andExpect(jsonPath("$.data.moodRatio[0].mood").value("A"))
                .andExpect(jsonPath("$.data.moodRatio[0].count").value(1));
    }
}
