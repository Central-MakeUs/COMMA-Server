package com.cmc.comma.domain.user.dto.response;

import com.cmc.comma.domain.user.entity.Plan;
import java.util.Arrays;
import java.util.List;

/**
 * 구독 요금제 화면.
 * currentPlan: 현재 선택한 플랜 / plans: 무료·프리미엄 카드 목록(selected로 현재 여부 표시).
 */
public record PlanResponse(
        Plan currentPlan,
        List<PlanCard> plans
) {
    public record PlanCard(
            Plan plan,
            String label,
            String description,
            boolean selected
    ) {}

    public static PlanResponse of(Plan currentPlan) {
        List<PlanCard> cards = Arrays.stream(Plan.values())
                .map(p -> new PlanCard(p, p.getLabel(), p.getDescription(), p == currentPlan))
                .toList();
        return new PlanResponse(currentPlan, cards);
    }
}
