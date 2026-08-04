package com.cmc.comma.domain.mypage.service;

import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.activity.repository.ActivityRepository.MoodCount;
import com.cmc.comma.domain.activity.repository.ActivityRepository.RelaxCount;
import com.cmc.comma.domain.activity.repository.ActivityRepository.TimeBudgetCount;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.mypage.dto.response.MyReportResponse;
import com.cmc.comma.domain.mypage.dto.response.MyReportResponse.ActivityRank;
import com.cmc.comma.domain.mypage.dto.response.MyReportResponse.MoodRatio;
import com.cmc.comma.domain.mypage.dto.response.MyReportResponse.TimeBudgetRatio;
import com.cmc.comma.domain.relax.entity.Relax;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import com.cmc.comma.global.storage.StorageService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyReportService {

    private final ActivityRepository activityRepository;
    private final RelaxRepository relaxRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public MyReportResponse getReport(Long userId) {
        return new MyReportResponse(activityRanking(userId), moodRatio(userId), timeBudgetRatio(userId));
    }

    /** 활동 순위: relaxId별 누적 횟수(내림차순) 집계 후 활동명+이미지 매핑. */
    private List<ActivityRank> activityRanking(Long userId) {
        List<RelaxCount> counts = activityRepository.countByUserIdGroupByRelaxId(userId);
        Map<Long, Relax> relaxes = relaxRepository.findAllById(
                        counts.stream().map(RelaxCount::getRelaxId).toList()).stream()
                .collect(Collectors.toMap(Relax::getId, relax -> relax));

        List<ActivityRank> ranking = new java.util.ArrayList<>();
        int rank = 1;
        for (RelaxCount c : counts) {
            Relax relax = relaxes.get(c.getRelaxId());
            String name = relax == null ? null : relax.getName();
            String imageUrl = relax == null ? null : storageService.publicUrl(relax.getImageKey());
            ranking.add(new ActivityRank(rank++, c.getRelaxId(), name, imageUrl, c.getCount()));
        }
        return ranking;
    }

    /** 무드 비율: 순위와 별개로 무드 분포를 집계. A/B/C 3가지 항상 포함(없으면 0). */
    private List<MoodRatio> moodRatio(Long userId) {
        Map<Mood, Long> counts = activityRepository.countByUserIdGroupByMood(userId).stream()
                .collect(Collectors.toMap(MoodCount::getMood, MoodCount::getCount));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        return java.util.Arrays.stream(Mood.values())
                .map(mood -> {
                    long count = counts.getOrDefault(mood, 0L);
                    double ratio = total == 0 ? 0.0 : Math.round(count * 1000.0 / total) / 10.0;
                    return new MoodRatio(mood, mood.getLabel(), count, ratio);
                })
                .toList();
    }

    /** 시간 비율: 순위와 별개로 timeBudget 분포를 집계. X/Y/Z 3가지 항상 포함(없으면 0). */
    private List<TimeBudgetRatio> timeBudgetRatio(Long userId) {
        Map<TimeBudget, Long> counts = activityRepository.countByUserIdGroupByTimeBudget(userId).stream()
                .collect(Collectors.toMap(TimeBudgetCount::getTimeBudget, TimeBudgetCount::getCount));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        return java.util.Arrays.stream(TimeBudget.values())
                .map(timeBudget -> {
                    long count = counts.getOrDefault(timeBudget, 0L);
                    double ratio = total == 0 ? 0.0 : Math.round(count * 1000.0 / total) / 10.0;
                    return new TimeBudgetRatio(timeBudget, timeBudget.getLabel(), count, ratio);
                })
                .toList();
    }
}
