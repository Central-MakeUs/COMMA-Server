package com.cmc.comma.domain.relax.service;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.activity.repository.ActivityRepository;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.dto.response.RelaxResponse;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import com.cmc.comma.global.storage.StorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
     * 이전에 시작만 하고 완료 안 한 활동이 있어도 막지 않는다 — 시작만 하고 이탈하는 건 흔한
     * 경우라 매번 새 activityId를 발급해준다. 완료 안 된 활동은 어차피 마이 리포트 집계에서
     * 빠지므로(완료된 것만 집계) 그냥 방치해도 무해하다.
     */
    @Transactional
    public Long startRelax(Long userId, Long relaxId) {
        if (!relaxRepository.existsById(relaxId)) {
            throw new CommaException(ErrorCode.REST_RECOMMEND_NOT_FOUND);
        }
        return activityRepository.save(Activity.start(userId, relaxId)).getId();
    }
}
