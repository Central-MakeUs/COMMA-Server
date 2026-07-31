package com.cmc.comma.domain.user.service;

import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.auth.repository.RefreshTokenRepository;
import com.cmc.comma.domain.feed.entity.Feed;
import com.cmc.comma.domain.feed.repository.FeedBlockRepository;
import com.cmc.comma.domain.feed.repository.FeedLikeRepository;
import com.cmc.comma.domain.feed.repository.FeedReportRepository;
import com.cmc.comma.domain.feed.repository.FeedRepository;
import com.cmc.comma.domain.user.dto.response.PlanResponse;
import com.cmc.comma.domain.user.entity.ContactType;
import com.cmc.comma.domain.user.entity.Plan;
import com.cmc.comma.domain.user.entity.PremiumAlert;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.PremiumAlertRepository;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import com.cmc.comma.global.storage.StorageService;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_GENERATE_ATTEMPTS = 10;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{1,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[0-9]-?\\d{3,4}-?\\d{4}$");

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;
    private final PremiumAlertRepository premiumAlertRepository;
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedReportRepository feedReportRepository;
    private final FeedBlockRepository feedBlockRepository;
    private final ActivityRepository activityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StorageService storageService;

    /**
     * 중복 없는 랜덤 닉네임 추천. 생성 → DB 조회 후 겹치면 재시도.
     * (최종 유일성 보장은 nickname 컬럼의 유니크 제약이 담당)
     */
    @Transactional(readOnly = true)
    public String generateUniqueNickname() {
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String candidate = nicknameGenerator.generate();
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        validateNickname(nickname);
        if (userRepository.existsByNickname(nickname)) {
            throw new CommaException(ErrorCode.DUPLICATE_NICKNAME);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));
        user.completeOnboarding(nickname);

        // 사전 조회를 통과했더라도 동시에 같은 닉네임이 저장되면 유니크 제약이 막는다.
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CommaException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateNickname(String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new CommaException(ErrorCode.INVALID_NICKNAME);
        }
    }

    /** 구독 요금제 조회 (현재 플랜 + 무료/프리미엄 카드). */
    @Transactional(readOnly = true)
    public PlanResponse getPlan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));
        return PlanResponse.of(user.getPlan());
    }

    /** 구독 요금제 변경. */
    @Transactional
    public PlanResponse changePlan(Long userId, Plan plan) {
        if (plan == null) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));
        user.changePlan(plan);
        return PlanResponse.of(plan);
    }

    /** 프리미엄 출시 알림 신청 (유저당 1건, 재제출 시 갱신). */
    @Transactional
    public void savePremiumAlert(Long userId, ContactType contactType, String contact) {
        String normalized = validateContact(contactType, contact);
        premiumAlertRepository.findByUserId(userId).ifPresentOrElse(
                alert -> alert.update(contactType, normalized),
                () -> premiumAlertRepository.save(PremiumAlert.of(userId, contactType, normalized)));
    }

    /** 회원 탈퇴: 내 데이터(피드·좋아요·신고·차단·활동·알림·토큰)와 업로드 이미지까지 하드 삭제. */
    @Transactional
    public void withdraw(Long userId) {
        List<Feed> myFeeds = feedRepository.findByUserId(userId);
        // 업로드 이미지 정리 (best-effort)
        myFeeds.forEach(feed -> storageService.delete(feed.getImageKey()));

        // 좋아요/신고/차단 정리: 내 피드에 달린 것 + 내가 남긴 것
        List<Long> myFeedIds = myFeeds.stream().map(Feed::getId).toList();
        if (!myFeedIds.isEmpty()) {
            feedLikeRepository.deleteByFeedIdIn(myFeedIds);
            feedReportRepository.deleteByFeedIdIn(myFeedIds);
            feedBlockRepository.deleteByFeedIdIn(myFeedIds);
        }
        feedLikeRepository.deleteByUserId(userId);
        feedReportRepository.deleteByReporterId(userId);
        feedBlockRepository.deleteByUserId(userId);

        feedRepository.deleteByUserId(userId);
        activityRepository.deleteByUserId(userId);
        premiumAlertRepository.deleteByUserId(userId);
        refreshTokenRepository.delete(userId);
        userRepository.deleteById(userId);
    }

    private String validateContact(ContactType contactType, String contact) {
        if (contactType == null || contact == null || contact.isBlank()) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }
        String value = contact.strip();
        boolean valid = switch (contactType) {
            case EMAIL -> EMAIL_PATTERN.matcher(value).matches();
            case PHONE -> PHONE_PATTERN.matcher(value).matches();
        };
        if (!valid) {
            throw new CommaException(ErrorCode.INVALID_INPUT);
        }
        return value;
    }
}