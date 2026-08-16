package com.cmc.comma.domain.relax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.entity.Relax;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * "N명이 함께하는 중"(active-count), "1시간 내 접속자 수"(online-count) 집계가
 * 이벤트/호출 횟수가 아니라 실제 distinct 유저 수를 반영하는지 검증한다.
 */
class RelaxCountIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RelaxRepository relaxRepository;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("같은 유저가 같은 휴식을 두 번 시작해도 함께하는 중 인원은 1명이다")
    void activeCount_sameUserRestarting_countsAsOne() throws Exception {
        User user = createUser("함께휴식유저");
        Relax relax = seedRelax();
        String token = bearer(user.getId());

        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/relaxes/{relaxId}/active-count", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((Number) JsonPath.read(body, "$.data.count")).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 두 유저가 같은 휴식을 시작하면 함께하는 중 인원은 2명이다")
    void activeCount_twoDistinctUsers_countsAsTwo() throws Exception {
        User userA = createUser("유저A");
        User userB = createUser("유저B");
        Relax relax = seedRelax();

        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userA.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userB.getId())))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/relaxes/{relaxId}/active-count", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userA.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((Number) JsonPath.read(body, "$.data.count")).isEqualTo(2);
    }

    @Test
    @DisplayName("온라인 카운트 화면을 안 열어도, 인증된 다른 요청만으로 접속자로 집계된다")
    void onlineCount_reflectsActivityFromAnyAuthenticatedRequest_notJustItself() throws Exception {
        User user = createUser("휴식만하는유저");
        Relax relax = seedRelax();
        assertThat(user.getLastActiveAt()).isNull();

        // online-count는 한 번도 안 부르고, 휴식 시작만 한다.
        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk());

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getLastActiveAt()).isNotNull();
    }
}
