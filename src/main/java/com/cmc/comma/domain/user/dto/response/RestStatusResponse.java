package com.cmc.comma.domain.user.dto.response;

import com.cmc.comma.domain.user.entity.User;
import java.time.LocalDateTime;

/** 홈 화면 휴식 유도 배너 개인화용. restedToday=true면 오늘 이미 휴식을 완료(피드 작성)한 것. */
public record RestStatusResponse(
        boolean restedToday,
        LocalDateTime lastRestedAt
) {
    public static RestStatusResponse of(User user) {
        return new RestStatusResponse(user.hasRestedToday(), user.getLastRestedAt());
    }
}
