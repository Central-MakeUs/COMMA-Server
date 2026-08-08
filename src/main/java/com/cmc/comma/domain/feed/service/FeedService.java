package com.cmc.comma.domain.feed.service;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.dto.request.FeedCreateRequest;
import com.cmc.comma.domain.feed.dto.response.FeedListResponse;
import com.cmc.comma.domain.feed.dto.response.FeedResponse;
import com.cmc.comma.domain.feed.dto.response.LikeResponse;
import com.cmc.comma.domain.feed.entity.Feed;
import com.cmc.comma.domain.feed.entity.FeedBlock;
import com.cmc.comma.domain.feed.entity.FeedLike;
import com.cmc.comma.domain.feed.entity.FeedReport;
import com.cmc.comma.domain.feed.repository.FeedBlockRepository;
import com.cmc.comma.domain.feed.repository.FeedLikeRepository;
import com.cmc.comma.domain.feed.repository.FeedLikeRepository.FeedLikeCount;
import com.cmc.comma.domain.feed.repository.FeedRepository;
import com.cmc.comma.domain.feed.repository.FeedReportRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import com.cmc.comma.global.storage.StorageService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int HASHTAG_MAX_COUNT = 2;
    private static final int HASHTAG_MAX_LENGTH = 10;
    private static final int REVIEW_MAX_LENGTH = 20;
    private static final long FIRST_CURSOR = Long.MAX_VALUE;
    // IdNotIn 쿼리에 빈 리스트를 넘기면 예외가 나므로, 차단한 피드가 없을 때 채워 넣는 존재하지 않는 id
    private static final List<Long> NO_BLOCKED_FEEDS = List.of(-1L);

    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedReportRepository feedReportRepository;
    private final FeedBlockRepository feedBlockRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    /**
     * 휴식 인증 게시글 생성 (사진 업로드 + 해시태그 + 소감 + 공개여부).
     * activityId로 지정한 휴식 활동을 함께 완료 처리한다 — 마이 리포트는 이 완료 여부로 집계되므로,
     * 존재하지 않거나 내 활동이 아니거나 이미 완료된 activityId는 거부한다.
     */
    @Transactional
    public FeedResponse create(Long userId, MultipartFile image, FeedCreateRequest request) {
        if (request.mood() == null || request.timeBudget() == null || request.isPublic() == null
                || request.activityId() == null) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }
        Activity activity = activityRepository.findById(request.activityId())
                .orElseThrow(() -> new CommaException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!activity.getUserId().equals(userId)) {
            throw new CommaException(ErrorCode.FORBIDDEN);
        }
        if (activity.isCompleted()) {
            throw new CommaException(ErrorCode.ACTIVITY_ALREADY_COMPLETED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));

        List<String> hashtags = normalizeHashtags(request.hashtags());
        String review = normalizeReview(request.review());

        String imageKey = storageService.upload(image, "feeds/" + userId);
        Feed feed = feedRepository.save(Feed.create(userId, request.mood(), request.timeBudget(), imageKey,
                hashtags, review, request.isPublic(), activity.getId()));
        activity.complete();
        user.markRested(); // 홈 배너 개인화용 체크포인트 갱신

        return FeedResponse.of(feed, storageService.publicUrl(imageKey), user.getNickname(), 0L, false);
    }

    /** 피드 좋아요 토글. 이미 눌렀으면 취소, 아니면 등록. 자기 글도 가능. */
    @Transactional
    public LikeResponse toggleLike(Long userId, Long feedId) {
        if (!feedRepository.existsById(feedId)) {
            throw new CommaException(ErrorCode.FEED_NOT_FOUND);
        }
        boolean liked;
        if (feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)) {
            feedLikeRepository.deleteByFeedIdAndUserId(feedId, userId);
            liked = false;
        } else {
            feedLikeRepository.save(FeedLike.of(feedId, userId));
            liked = true;
        }
        return new LikeResponse(liked, feedLikeRepository.countByFeedId(feedId));
    }

    /** 피드 신고. 같은 유저가 같은 피드를 재신고하면 거부(1피드당 1회). */
    @Transactional
    public void report(Long userId, Long feedId) {
        if (!feedRepository.existsById(feedId)) {
            throw new CommaException(ErrorCode.FEED_NOT_FOUND);
        }
        if (feedReportRepository.existsByFeedIdAndReporterId(feedId, userId)) {
            throw new CommaException(ErrorCode.ALREADY_REPORTED);
        }
        feedReportRepository.save(FeedReport.of(feedId, userId));
    }

    /** 피드 차단(숨김) 토글. 유저 전체가 아니라 이 피드 하나만 내 목록에서 안 보이게 한다. */
    @Transactional
    public boolean toggleBlock(Long userId, Long feedId) {
        if (!feedRepository.existsById(feedId)) {
            throw new CommaException(ErrorCode.FEED_NOT_FOUND);
        }
        boolean blocked;
        if (feedBlockRepository.existsByFeedIdAndUserId(feedId, userId)) {
            feedBlockRepository.deleteByFeedIdAndUserId(feedId, userId);
            blocked = false;
        } else {
            feedBlockRepository.save(FeedBlock.of(feedId, userId));
            blocked = true;
        }
        return blocked;
    }

    /**
     * 전체 공개 피드 (최신순 커서 페이징).
     * mood / timeBudget은 각각 선택 필터 — 둘 다, 하나만, 또는 없이 조회 가능.
     */
    @Transactional(readOnly = true)
    public FeedListResponse getPublicFeeds(Long currentUserId, Mood mood, TimeBudget timeBudget, Long cursor, int size) {
        long cursorId = cursorOrFirst(cursor);
        PageRequest page = PageRequest.of(0, size);
        List<Long> blockedIds = blockedFeedIds(currentUserId);
        Slice<Feed> slice;
        if (mood != null && timeBudget != null) {
            slice = feedRepository.findByIsPublicTrueAndMoodAndTimeBudgetAndIdNotInAndIdLessThanOrderByIdDesc(
                    mood, timeBudget, blockedIds, cursorId, page);
        } else if (mood != null) {
            slice = feedRepository.findByIsPublicTrueAndMoodAndIdNotInAndIdLessThanOrderByIdDesc(
                    mood, blockedIds, cursorId, page);
        } else if (timeBudget != null) {
            slice = feedRepository.findByIsPublicTrueAndTimeBudgetAndIdNotInAndIdLessThanOrderByIdDesc(
                    timeBudget, blockedIds, cursorId, page);
        } else {
            slice = feedRepository.findByIsPublicTrueAndIdNotInAndIdLessThanOrderByIdDesc(blockedIds, cursorId, page);
        }
        return toListResponse(slice, currentUserId);
    }

    /** 내 피드 (공개+비공개, 최신순 커서 페이징). */
    @Transactional(readOnly = true)
    public FeedListResponse getMyFeeds(Long userId, Long cursor, int size) {
        Slice<Feed> slice = feedRepository.findByUserIdAndIdNotInAndIdLessThanOrderByIdDesc(
                userId, blockedFeedIds(userId), cursorOrFirst(cursor), PageRequest.of(0, size));
        return toListResponse(slice, userId);
    }

    /** 이 유저가 차단한 피드 id 목록. 없으면 IdNotIn 예외를 피하려 존재하지 않는 sentinel을 채워 반환. */
    private List<Long> blockedFeedIds(Long userId) {
        List<Long> blocked = feedBlockRepository.findBlockedFeedIds(userId);
        return blocked.isEmpty() ? NO_BLOCKED_FEEDS : blocked;
    }

    private FeedListResponse toListResponse(Slice<Feed> slice, Long currentUserId) {
        List<Feed> feeds = slice.getContent();
        if (feeds.isEmpty()) {
            return new FeedListResponse(List.of(), null, false);
        }
        List<Long> feedIds = feeds.stream().map(Feed::getId).toList();

        // 작성자 닉네임을 userId로 한 번에 조회(N+1 방지) 후 매핑
        Map<Long, String> nicknames = userRepository.findAllById(
                        feeds.stream().map(Feed::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
        // 피드별 좋아요 수 배치 집계
        Map<Long, Long> likeCounts = feedLikeRepository.countByFeedIdIn(feedIds).stream()
                .collect(Collectors.toMap(FeedLikeCount::getFeedId, FeedLikeCount::getCount));
        // 현재 유저가 좋아요한 피드 id들 배치 조회
        Set<Long> likedByMe = new HashSet<>(feedLikeRepository.findLikedFeedIds(currentUserId, feedIds));

        List<FeedResponse> items = feeds.stream()
                .map(feed -> FeedResponse.of(feed, storageService.publicUrl(feed.getImageKey()),
                        nicknames.get(feed.getUserId()),
                        likeCounts.getOrDefault(feed.getId(), 0L),
                        likedByMe.contains(feed.getId())))
                .toList();
        Long nextCursor = slice.hasNext() && !items.isEmpty()
                ? items.get(items.size() - 1).feedId()
                : null;
        return new FeedListResponse(items, nextCursor, slice.hasNext());
    }

    private long cursorOrFirst(Long cursor) {
        return cursor == null ? FIRST_CURSOR : cursor;
    }

    private List<String> normalizeHashtags(List<String> hashtags) {
        if (hashtags == null) {
            return List.of();
        }
        List<String> cleaned = hashtags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.strip().replaceFirst("^#", ""))
                .filter(tag -> !tag.isBlank())
                .toList();
        if (cleaned.size() > HASHTAG_MAX_COUNT) {
            throw new CommaException(ErrorCode.HASHTAG_LIMIT_EXCEEDED);
        }
        cleaned.forEach(tag -> {
            if (tag.length() > HASHTAG_MAX_LENGTH) {
                throw new CommaException(ErrorCode.HASHTAG_TOO_LONG);
            }
        });
        return cleaned;
    }

    private String normalizeReview(String review) {
        if (review == null) {
            return null;
        }
        String trimmed = review.strip();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > REVIEW_MAX_LENGTH) {
            throw new CommaException(ErrorCode.REVIEW_TOO_LONG);
        }
        return trimmed;
    }
}