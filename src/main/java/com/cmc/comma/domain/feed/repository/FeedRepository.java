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

    // 전체 공개 피드 (필터 없음)
    Slice<Feed> findByIsPublicTrueAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    // 기분 + 시간 둘 다 필터
    Slice<Feed> findByIsPublicTrueAndMoodAndTimeBudgetAndIdLessThanOrderByIdDesc(
            Mood mood, TimeBudget timeBudget, Long cursorId, Pageable pageable);

    // 기분만 필터
    Slice<Feed> findByIsPublicTrueAndMoodAndIdLessThanOrderByIdDesc(
            Mood mood, Long cursorId, Pageable pageable);

    // 시간만 필터
    Slice<Feed> findByIsPublicTrueAndTimeBudgetAndIdLessThanOrderByIdDesc(
            TimeBudget timeBudget, Long cursorId, Pageable pageable);

    // 내 피드 (공개+비공개)
    Slice<Feed> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    // 회원 탈퇴 시: 내 피드 전체 조회(이미지 정리용) + 삭제
    List<Feed> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}