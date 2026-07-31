package com.cmc.comma.domain.feed.repository;

import com.cmc.comma.domain.feed.entity.FeedBlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedBlockRepository extends JpaRepository<FeedBlock, Long> {

    boolean existsByFeedIdAndUserId(Long feedId, Long userId);

    void deleteByFeedIdAndUserId(Long feedId, Long userId);

    // 이 유저가 차단한 피드 id 전체 목록 — 피드 목록 조회 시 제외 필터로 사용
    @Query("select fb.feedId from FeedBlock fb where fb.userId = :userId")
    List<Long> findBlockedFeedIds(Long userId);

    // 회원 탈퇴: 내가 차단한 기록 + 내 피드에 걸린 차단 기록 정리
    void deleteByUserId(Long userId);

    void deleteByFeedIdIn(List<Long> feedIds);
}
