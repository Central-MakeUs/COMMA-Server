package com.cmc.comma.domain.feed.dto.response;

/**
 * 좋아요 토글 결과.
 * liked: 이번 동작 후 상태(true=등록됨, false=취소됨) / likeCount: 갱신된 좋아요 수.
 */
public record LikeResponse(
        boolean liked,
        long likeCount
) {}