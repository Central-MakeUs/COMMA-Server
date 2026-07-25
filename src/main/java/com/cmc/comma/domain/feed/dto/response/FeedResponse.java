package com.cmc.comma.domain.feed.dto.response;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.entity.Feed;
import java.time.LocalDateTime;
import java.util.List;

public record FeedResponse(
        Long feedId,
        String nickname,
        Mood mood,
        TimeBudget timeBudget,
        String imageUrl,
        List<String> hashtags,
        String review,
        boolean isPublic,
        long likeCount,
        boolean isLiked,
        LocalDateTime createdAt
) {
    public static FeedResponse of(Feed feed, String imageUrl, String nickname, long likeCount, boolean isLiked) {
        return new FeedResponse(
                feed.getId(),
                nickname,
                feed.getMood(),
                feed.getTimeBudget(),
                imageUrl,
                feed.getHashtags(),
                feed.getReview(),
                feed.isPublic(),
                likeCount,
                isLiked,
                feed.getCreatedAt()
        );
    }
}