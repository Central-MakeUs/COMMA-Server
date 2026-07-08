package com.cmc.comma.domain.user.controller;

import com.cmc.comma.domain.user.dto.request.NicknameRequest;
import com.cmc.comma.domain.user.dto.response.NicknameResponse;
import com.cmc.comma.domain.user.service.UserService;
import com.cmc.comma.global.response.ApiResponse;
import com.cmc.comma.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
}