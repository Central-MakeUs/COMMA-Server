package com.cmc.comma.domain.feed.controller;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.feed.dto.request.FeedCreateRequest;
import com.cmc.comma.domain.feed.dto.response.FeedListResponse;
import com.cmc.comma.domain.feed.dto.response.FeedResponse;
import com.cmc.comma.domain.feed.service.FeedService;
import com.cmc.comma.global.response.ApiResponse;
import com.cmc.comma.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /** 휴식 인증 게시글 생성 (multipart: image 파일 + request JSON). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FeedResponse>> create(
            @RequestPart("image") MultipartFile image,
            @RequestPart("request") FeedCreateRequest request) {
        FeedResponse response = feedService.create(SecurityUtil.getCurrentUserId(), image, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * 전체 공개 피드 (커서 페이징).
     * mood + timeBudget을 함께 주면 해당 카테고리로 필터링한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<FeedListResponse>> getPublicFeeds(
            @RequestParam(required = false) Mood mood,
            @RequestParam(required = false) TimeBudget timeBudget,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        FeedListResponse response = (mood != null || timeBudget != null)
                ? feedService.getPublicFeedsByCategory(mood, timeBudget, cursor, size)
                : feedService.getPublicFeeds(cursor, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** 내 피드 (공개+비공개). */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<FeedListResponse>> getMyFeeds(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(feedService.getMyFeeds(SecurityUtil.getCurrentUserId(), cursor, size)));
    }

    /** 게시글 상세. */
    @GetMapping("/{feedId}")
    public ResponseEntity<ApiResponse<FeedResponse>> get(@PathVariable Long feedId) {
        return ResponseEntity.ok(
                ApiResponse.ok(feedService.get(SecurityUtil.getCurrentUserId(), feedId)));
    }
}