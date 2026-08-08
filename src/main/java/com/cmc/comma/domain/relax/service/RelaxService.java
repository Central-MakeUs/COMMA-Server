package com.cmc.comma.domain.relax.service;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.dto.response.InProgressResponse;
import com.cmc.comma.domain.relax.dto.response.RelaxResponse;
import com.cmc.comma.domain.relax.entity.Relax;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import com.cmc.comma.global.storage.StorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RelaxService {

    // "최근 1시간" 기준 윈도우 — 온라인 유저 수 / 동시 활동 유저 수 집계에 공통 사용
    private static final Duration ACTIVE_WINDOW = Duration.ofHours(1);

    private final RelaxRepository relaxRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /**
     * 체크리스트 답변 조합(mood, time)에 해당하는 휴식 5개 추천.
     * 각 휴식마다 "최근 1시간 내 시작한 유저 수"를 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public List<RelaxResponse> recommend(Mood mood, TimeBudget timeBudget) {
        LocalDateTime since = LocalDateTime.now().minus(ACTIVE_WINDOW);
        return relaxRepository.findByMoodAndTimeBudget(mood, timeBudget).stream()
                .map(relax -> RelaxResponse.of(
                        relax,
                        activityRepository.countByRelaxIdAndStartedAtAfter(relax.getId(), since),
                        storageService.publicUrl(relax.getImageKey())))
                .toList();
    }

    /**
     * 최근 1시간 내 접속한 유저 수. 호출한 유저의 lastActiveAt을 갱신한 뒤 집계한다.
     */
    @Transactional
    public long getOnlineCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));
        user.updateLastActive();
        userRepository.flush();
        return userRepository.countByLastActiveAtAfter(LocalDateTime.now().minus(ACTIVE_WINDOW));
    }

    /**
     * 특정 휴식을 최근 1시간 내 시작한 유저 수.
     */
    @Transactional(readOnly = true)
    public long getActiveCount(Long relaxId) {
        return activityRepository.countByRelaxIdAndStartedAtAfter(
                relaxId, LocalDateTime.now().minus(ACTIVE_WINDOW));
    }

    /**
     * 휴식 시작하기. 시작 기록(Activity)을 남기고 그 id를 반환한다.
     * 클라이언트는 이 id를 들고 있다가 완료 시(피드 작성) activityId로 넘겨야 한다.
     * 완료 전(진행 중) 활동이 이미 있으면 새로 시작할 수 없다 — 완료(피드 작성) 또는 별도 처리 없이는
     * 완료되지 않은 활동이 계속 쌓이는 걸 막기 위함.
     */
    @Transactional
    public Long startRelax(Long userId, Long relaxId) {
        if (!relaxRepository.existsById(relaxId)) {
            throw new CommaException(ErrorCode.REST_RECOMMEND_NOT_FOUND);
        }
        if (activityRepository.existsByUserIdAndCompletedAtIsNull(userId)) {
            throw new CommaException(ErrorCode.ACTIVITY_ALREADY_IN_PROGRESS);
        }
        return activityRepository.save(Activity.start(userId, relaxId)).getId();
    }

    /**
     * 내가 지금 진행 중(완료 전)인 휴식 활동. 없으면 empty.
     * 클라이언트가 activityId를 잃어버렸을 때(앱 재시작 등) 이걸로 복구한다.
     */
    @Transactional(readOnly = true)
    public Optional<InProgressResponse> getInProgress(Long userId) {
        return activityRepository.findByUserIdAndCompletedAtIsNull(userId)
                .map(activity -> {
                    Relax relax = relaxRepository.findById(activity.getRelaxId())
                            .orElseThrow(() -> new CommaException(ErrorCode.REST_RECOMMEND_NOT_FOUND));
                    return InProgressResponse.of(activity, relax, storageService.publicUrl(relax.getImageKey()));
                });
    }
}