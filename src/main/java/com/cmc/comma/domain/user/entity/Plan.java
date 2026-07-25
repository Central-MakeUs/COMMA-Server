package com.cmc.comma.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 구독 요금제. 신규 유저는 FREE로 시작한다.
 * (프리미엄 정식 출시 전이므로 결제 로직 없이 플랜 선택 값만 관리)
 */
@Getter
@RequiredArgsConstructor
public enum Plan {

    FREE("무료 플랜", "기본 휴식 추천과 피드 기능을 이용할 수 있어요."),
    PREMIUM("프리미엄 플랜", "맞춤 추천 등 프리미엄 기능을 이용할 수 있어요. (출시 예정)");

    private final String label;
    private final String description;
}
