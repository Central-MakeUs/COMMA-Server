package com.cmc.comma.domain.relax.dto.response;

/** 휴식 시작 응답. activityId는 완료(피드 작성) 시 FeedCreateRequest.activityId로 넘겨야 한다. */
public record StartResponse(Long activityId) {}
