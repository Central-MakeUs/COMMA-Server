package com.cmc.comma.domain.checklist.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Q1. 지금 상태가 어때요?
 */
@Getter
@RequiredArgsConstructor
public enum Mood {

    A("지치고 무기력해"),
    B("답답하고 환기가 필요해"),
    C("괜찮아, 가볍게 즐기고 싶어");

    private final String label;
}