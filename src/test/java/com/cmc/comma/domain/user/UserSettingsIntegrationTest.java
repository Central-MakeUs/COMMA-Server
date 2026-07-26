package com.cmc.comma.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class UserSettingsIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("닉네임 수정 성공")
    void updateNickname_success() throws Exception {
        User user = createUser("올드닉");

        mockMvc.perform(patch("/api/users/nickname")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"뉴닉네임\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("뉴닉네임"));
    }

    @Test
    @DisplayName("이미 존재하는 닉네임으로 수정 시 409")
    void updateNickname_duplicate_conflict() throws Exception {
        createUser("점유중");
        User me = createUser("나");

        mockMvc.perform(patch("/api/users/nickname")
                        .header(HttpHeaders.AUTHORIZATION, bearer(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"점유중\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("형식에 맞지 않는 닉네임(특수문자)은 400")
    void updateNickname_invalid_badRequest() throws Exception {
        User user = createUser("정상닉");

        mockMvc.perform(patch("/api/users/nickname")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"bad!!name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("요금제 조회 기본값 FREE → PREMIUM으로 변경")
    void getAndChangePlan() throws Exception {
        User user = createUser("요금제유저");
        String token = bearer(user.getId());

        mockMvc.perform(get("/api/users/me/plan")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPlan").value("FREE"))
                .andExpect(jsonPath("$.data.plans.length()").value(2));

        mockMvc.perform(patch("/api/users/me/plan")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PREMIUM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPlan").value("PREMIUM"));
    }
}
