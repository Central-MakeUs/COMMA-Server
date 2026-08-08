package com.cmc.comma.domain.activity.scheduler;

import com.cmc.comma.domain.activity.repository.ActivityRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시작만 하고 끝내 완료(피드 작성) 안 된 채로 방치된 Activity를 주기적으로 정리한다.
 * "N명이 함께하는 중" 집계는 최근 1시간만 보므로, 그보다 훨씬 지난 것만 지워도 그 집계엔 영향이 없다.
 * 완료된 활동(마이 리포트 집계 대상)은 이 배치가 절대 건드리지 않는다(completedAt is null만 대상).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityCleanupScheduler {

    // "함께하는 중" 창(1시간)보다 넉넉히 여유를 둔 보관 기간 — 지연 완료 케이스를 위한 버퍼.
    private static final Duration RETENTION = Duration.ofHours(24);

    private final ActivityRepository activityRepository;

    @Transactional
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul") // 매일 새벽 4시
    public void deleteStaleUncompletedActivities() {
        LocalDateTime cutoff = LocalDateTime.now().minus(RETENTION);
        int deleted = activityRepository.deleteByCompletedAtIsNullAndStartedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("[ACTIVITY_CLEANUP] 완료 안 된 채 {}h 지난 활동 {}건 삭제", RETENTION.toHours(), deleted);
        }
    }
}
