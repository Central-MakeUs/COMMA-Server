package com.cmc.comma.domain.user.entity;

import com.cmc.comma.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프리미엄 정식 출시 알림 신청. 유저당 1건(userId 유니크)으로 재제출 시 갱신한다.
 * contactType(EMAIL/PHONE) 중 하나로 받을 수단을 선택해 연락처 값을 저장한다.
 */
@Entity
@Table(name = "premium_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PremiumAlert extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactType contactType;

    @Column(nullable = false)
    private String contact;

    public static PremiumAlert of(Long userId, ContactType contactType, String contact) {
        return PremiumAlert.builder()
                .userId(userId)
                .contactType(contactType)
                .contact(contact)
                .build();
    }

    public void update(ContactType contactType, String contact) {
        this.contactType = contactType;
        this.contact = contact;
    }
}
