package com.cmc.comma.domain.relax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.entity.Relax;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class RelaxStartIntegrationTest extends IntegrationTestSupport {

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
    @DisplayName("휴식 시작하면 activityId를 응답으로 받는다")
    void start_returnsActivityId() throws Exception {
        User user = createUser("시작유저");
        Relax relax = seedRelax();

        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityId").exists());
    }

    @Test
    @DisplayName("완료 안 한 활동이 있어도 다시 시작할 수 있다 — 매번 새 activityId 발급")
    void start_ignoresPreviousUncompletedActivity() throws Exception {
        User user = createUser("재시작유저");
        Relax relax = seedRelax();
        String token = bearer(user.getId());

        String firstBody = mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondBody = mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number firstId = JsonPath.read(firstBody, "$.data.activityId");
        Number secondId = JsonPath.read(secondBody, "$.data.activityId");
        assertThat(secondId).isNotEqualTo(firstId);
    }
}
