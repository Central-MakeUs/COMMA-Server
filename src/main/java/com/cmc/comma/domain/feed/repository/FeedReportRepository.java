package com.cmc.comma.domain.feed.repository;

import com.cmc.comma.domain.feed.entity.FeedReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedReportRepository extends JpaRepository<FeedReport, Long> {

    boolean existsByFeedIdAndReporterId(Long feedId, Long reporterId);

    // 회원 탈퇴: 내가 신고한 기록 + 내 피드에 달린 신고 기록 정리
    void deleteByReporterId(Long reporterId);

    void deleteByFeedIdIn(List<Long> feedIds);
}
