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
 * 유저가 휴식을 시작한 기록. "N명이 함께하는 중" 집계에 사용된다.
 * (최근 1시간 내 startedAt 개수로 동시 활동 유저 수를 근사)
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

    public static Activity start(Long userId, Long relaxId) {
        return Activity.builder()
                .userId(userId)
                .relaxId(relaxId)
                .startedAt(LocalDateTime.now())
                .build();
    }
}