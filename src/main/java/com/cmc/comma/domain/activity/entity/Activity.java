package com.cmc.comma.domain.activity.entity;

import com.cmc.comma.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저가 휴식을 시작한 기록. "N명이 함께하는 중" 집계(startedAt 기준)에 사용된다.
 * completedAt이 채워져야 "완료된 기록"이며, 마이 리포트(활동 순위/무드·시간 비율)는
 * completedAt이 NULL이 아닌 것만 집계한다. 완료는 이 활동을 참조하는 피드 작성으로 이루어진다.
 */
@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Activity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long relaxId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    public static Activity start(Long userId, Long relaxId) {
        return Activity.builder()
                .userId(userId)
                .relaxId(relaxId)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    /** 이 활동을 완료 처리한다. 이미 완료됐는지는 호출부(FeedService)에서 먼저 확인한다. */
    public void complete() {
        this.completedAt = LocalDateTime.now();
    }
}