package com.cmc.comma.domain.feed;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.entity.Feed;
import com.cmc.comma.domain.feed.repository.FeedRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * GET /api/feeds는 전체 공개 피드를 반환하므로, 다른 테스트 클래스가 만든 데이터와
 * 같은 Testcontainers DB를 공유한다. 그래서 전체 개수(length())가 아니라
 * "특정 feedId가 목록에 있는지/없는지"로 검증한다 — 최신순 정렬이라 방금 만든 피드는
 * 항상 기본 페이지(size=20) 맨 앞쪽에 들어와 안전하게 검증 가능하다.
 */
class FeedBlockIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FeedRepository feedRepository;

    private Feed seedFeed(Long authorId) {
        return feedRepository.save(Feed.create(
                authorId, Mood.A, TimeBudget.X, "feeds/seed.jpg", List.of("태그"), "소감", true));
    }

    @Test
    @DisplayName("차단한 피드는 전체 공개 피드 목록에서 나에게만 안 보이고, 다시 누르면 해제된다")
    void toggleBlock_hidesFromMyListing_thenUnblockRestores() throws Exception {
        User author = createUser("차단대상작성자");
        User blocker = createUser("차단자");
        User other = createUser("제3자");
        Feed blockedFeed = seedFeed(author.getId());
        Feed visibleFeed = seedFeed(author.getId());
        String blockerToken = bearer(blocker.getId());

        // 차단 전: 둘 다 보임
        assertFeedPresence(blockerToken, blockedFeed.getId(), true);
        assertFeedPresence(blockerToken, visibleFeed.getId(), true);

        // 차단
        mockMvc.perform(post("/api/feeds/{feedId}/block", blockedFeed.getId())
                        .header(HttpHeaders.AUTHORIZATION, blockerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked").value(true));

        // 차단자에게는 그 피드만 안 보이고, 다른 피드는 여전히 보임
        assertFeedPresence(blockerToken, blockedFeed.getId(), false);
        assertFeedPresence(blockerToken, visibleFeed.getId(), true);

        // 제3자에게는 차단과 무관하게 여전히 보임 (개인별 차단)
        assertFeedPresence(bearer(other.getId()), blockedFeed.getId(), true);

        // 차단 해제(재클릭)
        mockMvc.perform(post("/api/feeds/{feedId}/block", blockedFeed.getId())
                        .header(HttpHeaders.AUTHORIZATION, blockerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked").value(false));

        assertFeedPresence(blockerToken, blockedFeed.getId(), true);
    }

    @Test
    @DisplayName("없는 피드 차단 시 404")
    void toggleBlock_notFound() throws Exception {
        User blocker = createUser("차단자2");

        mockMvc.perform(post("/api/feeds/{feedId}/block", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(blocker.getId())))
                .andExpect(status().isNotFound());
    }

    private void assertFeedPresence(String token, Long feedId, boolean expectedPresent) throws Exception {
        var result = mockMvc.perform(get("/api/feeds").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        String filter = "$.data.items[?(@.feedId == " + feedId + ")]";
        if (expectedPresent) {
            result.andExpect(jsonPath(filter).isNotEmpty());
        } else {
            result.andExpect(jsonPath(filter).isEmpty());
        }
    }
}
