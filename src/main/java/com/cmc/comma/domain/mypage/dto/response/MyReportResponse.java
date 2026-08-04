package com.cmc.comma.domain.mypage.dto.response;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import java.util.List;

/**
 * 내 휴식 리포트.
 * activityRanking: 내 휴식 활동을 누적 횟수 내림차순으로 정렬한 순위 카드 (별도 집계 1회).
 * moodRatio: 상태(Q1/mood) 답변 비율 — 순위와 무관하게 무드 분포를 별도로 1회 집계.
 *            무드 A/B/C 3가지가 항상 모두 포함되며, 활동이 없는 무드는 count=0, ratio=0.
 * timeBudgetRatio: 시간(Q2/timeBudget) 답변 비율 — moodRatio와 동일한 방식으로 별도 1회 집계.
 *                  X/Y/Z 3가지가 항상 모두 포함되며, 활동이 없는 항목은 count=0, ratio=0.
 */
public record MyReportResponse(
        List<ActivityRank> activityRanking,
        List<MoodRatio> moodRatio,
        List<TimeBudgetRatio> timeBudgetRatio
) {
    public record ActivityRank(
            int rank,
            Long relaxId,
            String name,
            String imageUrl,
            long count
    ) {}

    public record MoodRatio(
            Mood mood,
            String label,
            long count,
            double ratio  // 백분율(%) — 소수 첫째자리 반올림
    ) {}

    public record TimeBudgetRatio(
            TimeBudget timeBudget,
            String label,
            long count,
            double ratio  // 백분율(%) — 소수 첫째자리 반올림
    ) {}
}
