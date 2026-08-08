package com.cmc.comma.domain.relax.dto.response;

import com.cmc.comma.domain.activity.entity.Activity;
import com.cmc.comma.domain.relax.entity.Relax;
import java.time.LocalDateTime;

/**
 * 내가 지금 진행 중(완료 전)인 휴식 활동. 앱이 재시작되는 등으로 activityId를 잃어버렸을 때
 * 이 API로 복구한다. 진행 중인 활동이 없으면 이 자체가 아니라 data:null로 응답한다.
 */
public record InProgressResponse(
        Long activityId,
        Long relaxId,
        String name,
        String imageUrl,
        LocalDateTime startedAt
) {
    public static InProgressResponse of(Activity activity, Relax relax, String imageUrl) {
        return new InProgressResponse(
                activity.getId(),
                relax.getId(),
                relax.getName(),
                imageUrl,
                activity.getStartedAt()
        );
    }
}
