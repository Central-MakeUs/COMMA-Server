package com.cmc.comma.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

class UserRestStatusIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("한 번도 안 쉰 유저는 restedToday=false, lastRestedAt=null")
    void restStatus_never_rested() throws Exception {
        User user = createUser("휴식전유저");

        mockMvc.perform(get("/api/users/me/rest-status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restedToday").value(false))
                .andExpect(jsonPath("$.data.lastRestedAt").doesNotExist());
    }

    @Test
    @DisplayName("오늘 휴식을 완료한 유저는 restedToday=true")
    void restStatus_restedToday() throws Exception {
        User user = createUser("오늘휴식유저");
        user.markRested();
        userRepository.save(user);

        mockMvc.perform(get("/api/users/me/rest-status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restedToday").value(true))
                .andExpect(jsonPath("$.data.lastRestedAt").exists());
    }

    @Test
    @DisplayName("어제 휴식한 유저는 오늘 기준 restedToday=false")
    void restStatus_restedYesterday_notToday() throws Exception {
        User user = createUser("어제휴식유저");
        ReflectionTestUtils.setField(user, "lastRestedAt", LocalDateTime.now().minusDays(1));
        userRepository.save(user);

        mockMvc.perform(get("/api/users/me/rest-status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restedToday").value(false))
                .andExpect(jsonPath("$.data.lastRestedAt").exists());
    }
}
