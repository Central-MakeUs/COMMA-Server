package com.cmc.comma.domain.activity.repository;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    long countByRelaxIdAndStartedAtAfter(Long relaxId, LocalDateTime startedAt);

    // 마이 리포트 - 활동 순위: 내 휴식 활동을 relaxId별 누적 횟수로 집계(내림차순). 한 번에 조회.
    @Query("select a.relaxId as relaxId, count(a) as count from Activity a "
            + "where a.userId = :userId group by a.relaxId order by count(a) desc")
    List<RelaxCount> countByUserIdGroupByRelaxId(Long userId);

    // 마이 리포트 - 무드 비율: 순위와 별개로, 내 활동들의 relax.mood 분포를 한 번에 집계.
    @Query("select r.mood as mood, count(a) as count from Activity a, Relax r "
            + "where a.relaxId = r.id and a.userId = :userId group by r.mood")
    List<MoodCount> countByUserIdGroupByMood(Long userId);

    // 마이 리포트 - 시간 비율: 내 활동들의 relax.timeBudget 분포를 한 번에 집계.
    @Query("select r.timeBudget as timeBudget, count(a) as count from Activity a, Relax r "
            + "where a.relaxId = r.id and a.userId = :userId group by r.timeBudget")
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