package com.cmc.comma.domain.checklist.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Q1. 지금 기분이 어때요?
 */
@Getter
@RequiredArgsConstructor
public enum Mood {

    A("멍하고 싶어"),
    B("기분 전환이 필요해"),
    C("가볍게 해볼 수 있어");

    private final String label;
}