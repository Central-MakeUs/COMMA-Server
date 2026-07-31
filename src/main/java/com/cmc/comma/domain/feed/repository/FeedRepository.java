package com.cmc.comma.domain.feed.repository;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.entity.Feed;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // 커서(No-offset) 페이징: id 내림차순 = 최신순. 첫 페이지는 cursorId를 Long.MAX_VALUE로 넘긴다.
    // blockedIds: 이 유저가 차단한 피드 id 목록. 비어있으면 안 되므로(빈 IN절은 예외) 호출부에서
    // 항상 존재하지 않는 sentinel(-1L)을 채워 넘긴다 — FeedService.blockedFeedIds() 참고.

    // 전체 공개 피드 (필터 없음)
    Slice<Feed> findByIsPublicTrueAndIdNotInAndIdLessThanOrderByIdDesc(
            List<Long> blockedIds, Long cursorId, Pageable pageable);

    // 기분 + 시간 둘 다 필터
    Slice<Feed> findByIsPublicTrueAndMoodAndTimeBudgetAndIdNotInAndIdLessThanOrderByIdDesc(
            Mood mood, TimeBudget timeBudget, List<Long> blockedIds, Long cursorId, Pageable pageable);

    // 기분만 필터
    Slice<Feed> findByIsPublicTrueAndMoodAndIdNotInAndIdLessThanOrderByIdDesc(
            Mood mood, List<Long> blockedIds, Long cursorId, Pageable pageable);

    // 시간만 필터
    Slice<Feed> findByIsPublicTrueAndTimeBudgetAndIdNotInAndIdLessThanOrderByIdDesc(
            TimeBudget timeBudget, List<Long> blockedIds, Long cursorId, Pageable pageable);

    // 내 피드 (공개+비공개)
    Slice<Feed> findByUserIdAndIdNotInAndIdLessThanOrderByIdDesc(
            Long userId, List<Long> blockedIds, Long cursorId, Pageable pageable);

    // 회원 탈퇴 시: 내 피드 전체 조회(이미지 정리용) + 삭제
    List<Feed> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}