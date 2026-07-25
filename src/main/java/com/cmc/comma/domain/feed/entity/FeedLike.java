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
 * 피드 좋아요. (feedId, userId) 한 쌍이 곧 "이 유저가 이 피드를 좋아요함".
 * 누가 눌렀는지 화면에 노출하진 않지만, 토글(재클릭 취소)과 중복 방지를 위해 매핑만 저장한다.
 * 유니크 제약으로 한 유저가 같은 피드에 좋아요를 중복 등록할 수 없다.
 */
@Entity
@Table(name = "feed_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_feed_like", columnNames = {"feed_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public static FeedLike of(Long feedId, Long userId) {
        return FeedLike.builder().feedId(feedId).userId(userId).build();
    }
}
