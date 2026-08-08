package com.cmc.comma.domain.relax.controller;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.dto.response.CountResponse;
import com.cmc.comma.domain.relax.dto.response.InProgressResponse;
import com.cmc.comma.domain.relax.dto.response.RelaxResponse;
import com.cmc.comma.domain.relax.dto.response.StartResponse;
import com.cmc.comma.domain.relax.service.RelaxService;
import com.cmc.comma.global.response.ApiResponse;
import com.cmc.comma.global.util.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relaxes")
@RequiredArgsConstructor
public class RelaxController {

    private final RelaxService relaxService;

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<RelaxResponse>>> recommend(
            @RequestParam Mood mood,
            @RequestParam TimeBudget time) {
        return ResponseEntity.ok(ApiResponse.ok(relaxService.recommend(mood, time)));
    }

    @GetMapping("/online-count")
    public ResponseEntity<ApiResponse<CountResponse>> onlineCount() {
        long count = relaxService.getOnlineCount(SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(new CountResponse(count)));
    }

    @GetMapping("/{relaxId}/active-count")
    public ResponseEntity<ApiResponse<CountResponse>> activeCount(@PathVariable Long relaxId) {
        return ResponseEntity.ok(ApiResponse.ok(new CountResponse(relaxService.getActiveCount(relaxId))));
    }

    @PostMapping("/{relaxId}/start")
    public ResponseEntity<ApiResponse<StartResponse>> start(@PathVariable Long relaxId) {
        Long activityId = relaxService.startRelax(SecurityUtil.getCurrentUserId(), relaxId);
        return ResponseEntity.ok(ApiResponse.ok(new StartResponse(activityId)));
    }

    /** 내가 지금 진행 중(완료 전)인 휴식 활동. 없으면 data:null. activityId를 잃어버린 클라이언트의 복구용. */
    @GetMapping("/in-progress")
    public ResponseEntity<ApiResponse<InProgressResponse>> inProgress() {
        InProgressResponse response = relaxService.getInProgress(SecurityUtil.getCurrentUserId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}