package com.cmc.comma.domain.feed.repository;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // 커서(No-offset) 페이징: id 내림차순 = 최신순. 첫 페이지는 cursorId를 Long.MAX_VALUE로 넘긴다.

    // 전체 공개 피드
    Slice<Feed> findByIsPublicTrueAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    // 카테고리(기분+시간)별 공개 피드
    Slice<Feed> findByIsPublicTrueAndMoodAndTimeBudgetAndIdLessThanOrderByIdDesc(
            Mood mood, TimeBudget timeBudget, Long cursorId, Pageable pageable);

    // 내 피드 (공개+비공개)
    Slice<Feed> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);
}