package com.cmc.comma.domain.feed.service;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.dto.request.FeedCreateRequest;
import com.cmc.comma.domain.feed.dto.response.FeedListResponse;
import com.cmc.comma.domain.feed.dto.response.FeedResponse;
import com.cmc.comma.domain.feed.entity.Feed;
import com.cmc.comma.domain.feed.repository.FeedRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import com.cmc.comma.global.storage.StorageService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final FeedRepository feedRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;

    /** 휴식 인증 게시글 생성 (사진 업로드 + 해시태그 + 소감 + 공개여부). */
    @Transactional
    public FeedResponse create(Long userId, MultipartFile image, FeedCreateRequest request) {
        if (request.mood() == null || request.timeBudget() == null || request.isPublic() == null) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }
        List<String> hashtags = normalizeHashtags(request.hashtags());
        String review = normalizeReview(request.review());

        String imageKey = storageService.upload(image, "feeds/" + userId);
        Feed feed = feedRepository.save(Feed.create(
                userId, request.mood(), request.timeBudget(), imageKey, hashtags, review, request.isPublic()));

        return FeedResponse.of(feed, storageService.presignedUrl(imageKey), nickname(userId));
    }

    /** 게시글 상세. 비공개 글은 작성자 본인만 조회 가능. */
    @Transactional(readOnly = true)
    public FeedResponse get(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new CommaException(ErrorCode.FEED_NOT_FOUND));
        if (!feed.isPublic() && !feed.getUserId().equals(userId)) {
            throw new CommaException(ErrorCode.FORBIDDEN);
        }
        return FeedResponse.of(feed, storageService.presignedUrl(feed.getImageKey()), nickname(feed.getUserId()));
    }

    /** 전체 공개 피드 (최신순 커서 페이징). */
    @Transactional(readOnly = true)
    public FeedListResponse getPublicFeeds(Long cursor, int size) {
        Slice<Feed> slice = feedRepository.findByIsPublicTrueAndIdLessThanOrderByIdDesc(
                cursorOrFirst(cursor), PageRequest.of(0, size));
        return toListResponse(slice);
    }

    /** 카테고리(기분+시간)별 공개 피드 (최신순 커서 페이징). */
    @Transactional(readOnly = true)
    public FeedListResponse getPublicFeedsByCategory(Mood mood, TimeBudget timeBudget, Long cursor, int size) {
        if (mood == null || timeBudget == null) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }
        Slice<Feed> slice = feedRepository.findByIsPublicTrueAndMoodAndTimeBudgetAndIdLessThanOrderByIdDesc(
                mood, timeBudget, cursorOrFirst(cursor), PageRequest.of(0, size));
        return toListResponse(slice);
    }

    /** 내 피드 (공개+비공개, 최신순 커서 페이징). */
    @Transactional(readOnly = true)
    public FeedListResponse getMyFeeds(Long userId, Long cursor, int size) {
        Slice<Feed> slice = feedRepository.findByUserIdAndIdLessThanOrderByIdDesc(
                userId, cursorOrFirst(cursor), PageRequest.of(0, size));
        return toListResponse(slice);
    }

    private FeedListResponse toListResponse(Slice<Feed> slice) {
        List<Feed> feeds = slice.getContent();
        // 작성자 닉네임을 userId로 한 번에 조회(N+1 방지) 후 매핑
        Map<Long, String> nicknames = userRepository.findAllById(
                        feeds.stream().map(Feed::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<FeedResponse> items = feeds.stream()
                .map(feed -> FeedResponse.of(feed, storageService.presignedUrl(feed.getImageKey()),
                        nicknames.get(feed.getUserId())))
                .toList();
        Long nextCursor = slice.hasNext() && !items.isEmpty()
                ? items.get(items.size() - 1).feedId()
                : null;
        return new FeedListResponse(items, nextCursor, slice.hasNext());
    }

    /** userId로 작성자 닉네임 조회 (없으면 null). */
    private String nickname(Long userId) {
        return userRepository.findById(userId).map(User::getNickname).orElse(null);
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