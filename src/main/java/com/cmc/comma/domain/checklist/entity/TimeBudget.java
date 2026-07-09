package com.cmc.comma.domain.checklist.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Q2. 어느정도 시간이 있어요?
 */
@Getter
@RequiredArgsConstructor
public enum TimeBudget {

    X("잠깐", "1시간 이내"),
    Y("여유", "1-6시간 이내"),
    Z("넉넉", "6시간 이상");

    private final String label;
    private final String description;
}