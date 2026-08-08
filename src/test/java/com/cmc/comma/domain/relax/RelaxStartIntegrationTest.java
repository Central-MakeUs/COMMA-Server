package com.cmc.comma.domain.relax;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class RelaxStartIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RelaxRepository relaxRepository;

    @Autowired
    private ActivityRepository activityRepository;

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
    @DisplayName("진행 중(완료 전)인 활동이 있으면 새로 시작할 수 없다(409)")
    void start_alreadyInProgress_conflict() throws Exception {
        User user = createUser("중복시작유저");
        Relax relax = seedRelax();
        activityRepository.save(Activity.start(user.getId(), relax.getId()));

        mockMvc.perform(post("/api/relaxes/{relaxId}/start", relax.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("진행 중인 활동이 없으면 in-progress 조회는 data:null")
    void inProgress_none_returnsNull() throws Exception {
        User user = createUser("진행중없음유저");

        mockMvc.perform(get("/api/relaxes/in-progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("진행 중인 활동이 있으면 in-progress 조회로 activityId/relaxId를 복구할 수 있다")
    void inProgress_existing_returnsActivity() throws Exception {
        User user = createUser("진행중있음유저");
        Relax relax = seedRelax();
        Activity activity = activityRepository.save(Activity.start(user.getId(), relax.getId()));

        mockMvc.perform(get("/api/relaxes/in-progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityId").value(activity.getId()))
                .andExpect(jsonPath("$.data.relaxId").value(relax.getId()))
                .andExpect(jsonPath("$.data.name").value("낮잠"));
    }

    @Test
    @DisplayName("완료된 활동은 in-progress 조회에 나오지 않는다")
    void inProgress_completed_returnsNull() throws Exception {
        User user = createUser("완료된활동유저");
        Relax relax = seedRelax();
        Activity activity = Activity.start(user.getId(), relax.getId());
        activity.complete();
        activityRepository.save(activity);

        mockMvc.perform(get("/api/relaxes/in-progress")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
