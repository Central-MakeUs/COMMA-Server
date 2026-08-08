package com.cmc.comma.domain.user.entity;

import com.cmc.comma.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE;

    @Column(nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    private LocalDateTime lastActiveAt;

    // 홈 화면 휴식 유도 배너 개인화용 체크포인트 — 마지막으로 휴식을 완료(피드 작성)한 시각.
    private LocalDateTime lastRestedAt;

    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public void markRested() {
        this.lastRestedAt = LocalDateTime.now();
    }

    /** 오늘 이미 휴식을 완료했는지 (서버 로컬 날짜 기준). */
    public boolean hasRestedToday() {
        return lastRestedAt != null && lastRestedAt.toLocalDate().isEqual(LocalDate.now());
    }

    public void completeOnboarding(String nickname) {
        this.nickname = nickname;
        this.onboardingCompleted = true;
    }

    public void updateProfile(String nickname) {
        if (nickname != null) this.nickname = nickname;
    }

    public void changePlan(Plan plan) {
        this.plan = plan;
    }
}