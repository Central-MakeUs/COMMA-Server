package com.cmc.comma.domain.mypage.controller;

import com.cmc.comma.domain.mypage.dto.response.MyReportResponse;
import com.cmc.comma.domain.mypage.service.MyReportService;
import com.cmc.comma.global.response.ApiResponse;
import com.cmc.comma.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyReportService myReportService;

    /** 내 휴식 리포트 (활동 순위 + 무드 답변 비율). */
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<MyReportResponse>> getReport() {
        return ResponseEntity.ok(
                ApiResponse.ok(myReportService.getReport(SecurityUtil.getCurrentUserId())));
    }
}
