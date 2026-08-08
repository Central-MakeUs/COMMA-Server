package com.cmc.comma.domain.feed.dto.request;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import java.util.List;

/**
 * 휴식 인증 게시글 생성 요청 (multipart의 JSON 파트).
 * mood/timeBudget: 카테고리(필수) / hashtags: 최대 2개, 각 최대 10자 /
 * review: 최대 20자 / isPublic: 공개 여부(필수) /
 * activityId: 완료 처리할 휴식 활동 id(필수, POST /api/relaxes/{relaxId}/start 응답에서 받은 값).
 */
public record FeedCreateRequest(
        Mood mood,
        TimeBudget timeBudget,
        List<String> hashtags,
        String review,
        Boolean isPublic,
        Long activityId
) {}