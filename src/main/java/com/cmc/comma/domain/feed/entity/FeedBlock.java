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
 * 특정 피드에 대한 차단. (feedId, userId) 한 쌍이 "이 유저에게만 이 피드가 안 보임"을 뜻한다.
 * 유저 전체 차단이 아니라 게시물 단위 차단(숨김)이다. 좋아요와 동일하게 토글(재클릭 시 해제) 가능.
 */
@Entity
@Table(name = "feed_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_feed_block", columnNames = {"feed_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public static FeedBlock of(Long feedId, Long userId) {
        return FeedBlock.builder().feedId(feedId).userId(userId).build();
    }
}
