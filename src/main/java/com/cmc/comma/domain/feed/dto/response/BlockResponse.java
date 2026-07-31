package com.cmc.comma.domain.feed.dto.response;

/** 피드 차단 토글 결과. blocked: 이번 동작 후 상태(true=차단됨, false=차단 해제됨). */
public record BlockResponse(boolean blocked) {}
