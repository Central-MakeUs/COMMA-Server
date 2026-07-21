package com.cmc.comma.domain.feed.dto.response;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.entity.Feed;
import java.time.LocalDateTime;
import java.util.List;

public record FeedResponse(
        Long feedId,
        Mood mood,
        TimeBudget timeBudget,
        String imageUrl,
        List<String> hashtags,
        String review,
        boolean isPublic,
        LocalDateTime createdAt
) {
    public static FeedResponse of(Feed feed, String imageUrl) {
        return new FeedResponse(
                feed.getId(),
                feed.getMood(),
                feed.getTimeBudget(),
                imageUrl,
                feed.getHashtags(),
                feed.getReview(),
                feed.isPublic(),
                feed.getCreatedAt()
        );
    }
}