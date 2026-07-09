package com.cmc.comma.domain.relax.controller;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.dto.response.CountResponse;
import com.cmc.comma.domain.relax.dto.response.RelaxResponse;
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
    public ResponseEntity<ApiResponse<Void>> start(@PathVariable Long relaxId) {
        relaxService.startRelax(SecurityUtil.getCurrentUserId(), relaxId);
        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}