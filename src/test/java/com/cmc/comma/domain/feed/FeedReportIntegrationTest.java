package com.cmc.comma.domain.feed;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class FeedReportIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FeedRepository feedRepository;

    private Feed seedFeed(Long authorId) {
        return feedRepository.save(Feed.create(
                authorId, Mood.A, TimeBudget.X, "feeds/seed.jpg", List.of("태그"), "소감", true, null));
    }

    @Test
    @DisplayName("피드 신고 성공")
    void report_success() throws Exception {
        User author = createUser("신고대상작성자");
        User reporter = createUser("신고자");
        Feed feed = seedFeed(author.getId());

        mockMvc.perform(post("/api/feeds/{feedId}/report", feed.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(reporter.getId())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("같은 유저가 같은 피드를 재신고하면 409")
    void report_duplicate_conflict() throws Exception {
        User author = createUser("재신고대상작성자");
        User reporter = createUser("재신고자");
        Feed feed = seedFeed(author.getId());
        String token = bearer(reporter.getId());

        mockMvc.perform(post("/api/feeds/{feedId}/report", feed.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/feeds/{feedId}/report", feed.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("없는 피드 신고 시 404")
    void report_notFound() throws Exception {
        User reporter = createUser("신고자2");

        mockMvc.perform(post("/api/feeds/{feedId}/report", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(reporter.getId())))
                .andExpect(status().isNotFound());
    }
}
