package com.cmc.comma.domain.feed.entity;

import com.cmc.comma.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드 신고. (feedId, reporterId, 신고일시)만 저장한다 — 신고 사유 등 별도 입력은 없다.
 * 유니크 제약으로 한 유저가 같은 피드를 중복 신고할 수 없다(1피드당 1회).
 * 신고일시는 BaseTimeEntity의 createdAt으로 충분해 별도 컬럼을 두지 않는다.
 */
@Entity
@Table(name = "feed_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_feed_report", columnNames = {"feed_id", "reporter_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    public static FeedReport of(Long feedId, Long reporterId) {
        return FeedReport.builder().feedId(feedId).reporterId(reporterId).build();
    }
}
