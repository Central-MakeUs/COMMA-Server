package com.cmc.comma.domain.checklist.controller;

import com.cmc.comma.domain.checklist.dto.response.ChecklistResponse;
import com.cmc.comma.domain.checklist.service.ChecklistService;
import com.cmc.comma.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChecklistResponse>> getChecklist() {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.getChecklist()));
    }
}