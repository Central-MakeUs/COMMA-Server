package com.cmc.comma.domain.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.activity.scheduler.ActivityCleanupScheduler;
import com.cmc.comma.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ActivityCleanupSchedulerTest extends IntegrationTestSupport {

    @Autowired
    private ActivityCleanupScheduler scheduler;

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    @DisplayName("24시간 넘게 완료 안 된 활동만 삭제되고, 최근 활동/완료된 활동은 남는다")
    void deleteStaleUncompletedActivities() {
        LocalDateTime now = LocalDateTime.now();

        Activity stale = activityRepository.save(Activity.builder()
                .userId(1L).relaxId(1L).startedAt(now.minusHours(25)).build());
        Activity recent = activityRepository.save(Activity.builder()
                .userId(2L).relaxId(1L).startedAt(now.minusHours(1)).build());
        Activity staleButCompleted = Activity.builder()
                .userId(3L).relaxId(1L).startedAt(now.minusHours(25)).build();
        staleButCompleted.complete();
        activityRepository.save(staleButCompleted);

        scheduler.deleteStaleUncompletedActivities();

        assertThat(activityRepository.findById(stale.getId())).isEmpty();
        assertThat(activityRepository.findById(recent.getId())).isPresent();
        assertThat(activityRepository.findById(staleButCompleted.getId())).isPresent();
    }
}
