package com.cmc.comma.domain.feed.entity;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.global.common.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

/**
 * 휴식 인증 게시글. 사진(스토리지 key) + 해시태그 + 한줄 소감 + 공개여부.
 * 카테고리는 기분(Mood)+시간(TimeBudget) 조합으로, 전체 피드에서 이 조합으로 필터한다.
 * 공개(isPublic=true) 시 전체 피드에 노출, 비공개 시 작성자만 조회 가능.
 */
@Entity
@Table(name = "feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Feed extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // 카테고리 = 기분 + 시간 조합
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mood mood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeBudget timeBudget;

    // URL이 아니라 스토리지 객체 키만 저장. 조회 시 presigned URL로 변환한다.
    @Column(nullable = false)
    private String imageKey;

    @Column(length = 20)
    private String review;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "feed_hashtags", joinColumns = @JoinColumn(name = "feed_id"))
    @Column(name = "hashtag", length = 10)
    @BatchSize(size = 100)
    @Builder.Default
    private List<String> hashtags = new ArrayList<>();

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    public static Feed create(Long userId, Mood mood, TimeBudget timeBudget, String imageKey,
                              List<String> hashtags, String review, boolean isPublic) {
        return Feed.builder()
                .userId(userId)
                .mood(mood)
                .timeBudget(timeBudget)
                .imageKey(imageKey)
                .hashtags(hashtags == null ? new ArrayList<>() : new ArrayList<>(hashtags))
                .review(review)
                .isPublic(isPublic)
                .build();
    }
}