package com.cmc.comma.domain.user.controller;

import com.cmc.comma.domain.user.dto.request.NicknameRequest;
import com.cmc.comma.domain.user.dto.request.PlanRequest;
import com.cmc.comma.domain.user.dto.request.PremiumAlertRequest;
import com.cmc.comma.domain.user.dto.response.NicknameResponse;
import com.cmc.comma.domain.user.dto.response.PlanResponse;
import com.cmc.comma.domain.user.service.UserService;
import com.cmc.comma.global.response.ApiResponse;
import com.cmc.comma.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/nickname/random")
    public ResponseEntity<ApiResponse<NicknameResponse>> randomNickname() {
        return ResponseEntity.ok(ApiResponse.ok(
                new NicknameResponse(userService.generateUniqueNickname())));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<NicknameResponse>> updateNickname(
            @RequestBody NicknameRequest request) {
        userService.updateNickname(SecurityUtil.getCurrentUserId(), request.nickname());
        return ResponseEntity.ok(ApiResponse.ok(new NicknameResponse(request.nickname())));
    }

    /** 구독 요금제 조회 (현재 플랜 + 카드). */
    @GetMapping("/me/plan")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlan() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getPlan(SecurityUtil.getCurrentUserId())));
    }

    /** 구독 요금제 변경. */
    @PatchMapping("/me/plan")
    public ResponseEntity<ApiResponse<PlanResponse>> changePlan(@RequestBody PlanRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(userService.changePlan(SecurityUtil.getCurrentUserId(), request.plan())));
    }

    /** 프리미엄 출시 알림 신청 (이메일/연락처 중 하나). */
    @PostMapping("/me/premium-alert")
    public ResponseEntity<ApiResponse<Void>> premiumAlert(@RequestBody PremiumAlertRequest request) {
        userService.savePremiumAlert(SecurityUtil.getCurrentUserId(), request.contactType(), request.contact());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 회원 탈퇴. */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        userService.withdraw(SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}