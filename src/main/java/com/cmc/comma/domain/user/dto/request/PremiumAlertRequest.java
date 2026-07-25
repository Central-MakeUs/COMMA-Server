package com.cmc.comma.domain.user.dto.request;

import com.cmc.comma.domain.user.entity.ContactType;

/**
 * 프리미엄 출시 알림 신청. contactType(EMAIL/PHONE)에 맞는 contact 값을 입력받는다.
 */
public record PremiumAlertRequest(
        ContactType contactType,
        String contact
) {}
