package com.cmc.comma.domain.feed;

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

class FeedLikeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FeedRepository feedRepository;

    private Feed seedFeed(Long authorId) {
        return feedRepository.save(Feed.create(
                authorId, Mood.A, TimeBudget.X, "feeds/seed.jpg", List.of("태그"), "소감", true));
    }

    @Test
    @DisplayName("좋아요 토글: 처음 누르면 등록(count 1), 다시 누르면 취소(count 0)")
    void toggleLike() throws Exception {
        User author = createUser("작성자");
        User liker = createUser("좋아요유저");
        Feed feed = seedFeed(author.getId());
        String token = bearer(liker.getId());

        // 등록
        mockMvc.perform(post("/api/feeds/{feedId}/likes", feed.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // 취소
        mockMvc.perform(post("/api/feeds/{feedId}/likes", feed.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("자기 글에도 좋아요 가능")
    void toggleLike_ownFeed() throws Exception {
        User author = createUser("자기글작성자");
        Feed feed = seedFeed(author.getId());

        mockMvc.perform(post("/api/feeds/{feedId}/likes", feed.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(author.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("없는 피드에 좋아요 시 404")
    void toggleLike_notFound() throws Exception {
        User user = createUser("유저");

        mockMvc.perform(post("/api/feeds/{feedId}/likes", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.getId())))
                .andExpect(status().isNotFound());
    }
}
