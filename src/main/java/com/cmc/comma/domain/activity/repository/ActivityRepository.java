package com.cmc.comma.domain.activity.repository;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // "N명이 함께하는 중" 집계는 완료 여부와 무관하게 시작(startedAt) 기준 그대로 유지한다.
    long countByRelaxIdAndStartedAtAfter(Long relaxId, LocalDateTime startedAt);

    // 정리 배치: 시작만 하고 끝내 완료(피드 작성) 안 된 채로 오래 방치된 활동을 대량 삭제.
    // "함께하는 중" 집계 창(1시간)보다 한참 지난 것만 지우므로 그 집계에는 영향 없다.
    @Modifying
    @Query("delete from Activity a where a.completedAt is null and a.startedAt < :cutoff")
    int deleteByCompletedAtIsNullAndStartedAtBefore(LocalDateTime cutoff);

    // 마이 리포트 - 활동 순위: "완료된" 휴식 활동을 relaxId별 누적 횟수로 집계(내림차순). 한 번에 조회.
    @Query("select a.relaxId as relaxId, count(a) as count from Activity a "
            + "where a.userId = :userId and a.completedAt is not null group by a.relaxId order by count(a) desc")
    List<RelaxCount> countByUserIdGroupByRelaxId(Long userId);

    // 마이 리포트 - 무드 비율: 순위와 별개로, "완료된" 활동들의 relax.mood 분포를 한 번에 집계.
    @Query("select r.mood as mood, count(a) as count from Activity a, Relax r "
            + "where a.relaxId = r.id and a.userId = :userId and a.completedAt is not null group by r.mood")
    List<MoodCount> countByUserIdGroupByMood(Long userId);

    // 마이 리포트 - 시간 비율: "완료된" 활동들의 relax.timeBudget 분포를 한 번에 집계.
    @Query("select r.timeBudget as timeBudget, count(a) as count from Activity a, Relax r "
            + "where a.relaxId = r.id and a.userId = :userId and a.completedAt is not null group by r.timeBudget")
    List<TimeBudgetCount> countByUserIdGroupByTimeBudget(Long userId);

    void deleteByUserId(Long userId);

    interface RelaxCount {
        Long getRelaxId();

        long getCount();
    }

    interface MoodCount {
        Mood getMood();

        long getCount();
    }

    interface TimeBudgetCount {
        TimeBudget getTimeBudget();

        long getCount();
    }
}