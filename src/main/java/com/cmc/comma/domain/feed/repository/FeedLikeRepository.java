package com.cmc.comma.domain.feed.repository;

import com.cmc.comma.domain.feed.entity.FeedLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    // 토글: 내가 이 피드를 이미 좋아요했는지 / 취소
    boolean existsByFeedIdAndUserId(Long feedId, Long userId);

    void deleteByFeedIdAndUserId(Long feedId, Long userId);

    // 단건 좋아요 수
    long countByFeedId(Long feedId);

    // 목록 배치: 여러 피드의 좋아요 수를 한 번에 집계 (N+1 방지)
    @Query("select fl.feedId as feedId, count(fl) as count from FeedLike fl "
            + "where fl.feedId in :feedIds group by fl.feedId")
    List<FeedLikeCount> countByFeedIdIn(List<Long> feedIds);

    // 목록 배치: 이 유저가 좋아요한 피드 id들만 한 번에 조회
    @Query("select fl.feedId from FeedLike fl where fl.userId = :userId and fl.feedId in :feedIds")
    List<Long> findLikedFeedIds(Long userId, List<Long> feedIds);

    // 회원 탈퇴: 내가 누른 좋아요 삭제 + 내 피드들에 달린 좋아요 삭제
    void deleteByUserId(Long userId);

    void deleteByFeedIdIn(List<Long> feedIds);

    // countByFeedIdIn 결과 프로젝션
    interface FeedLikeCount {
        Long getFeedId();

        long getCount();
    }
}
