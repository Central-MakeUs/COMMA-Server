package com.cmc.comma.domain.feed.dto.response;

import java.util.List;

public record FeedListResponse(
        List<FeedResponse> items,
        Long nextCursor,   // null이면 마지막 페이지
        boolean hasNext
) {}