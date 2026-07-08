package com.cmc.comma.domain.user.service;

import com.cmc.comma.domain.user.entity.User;
import com.cmc.comma.domain.user.repository.UserRepository;
import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_GENERATE_ATTEMPTS = 10;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{1,10}$");

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    /**
     * 중복 없는 랜덤 닉네임 추천. 생성 → DB 조회 후 겹치면 재시도.
     * (최종 유일성 보장은 nickname 컬럼의 유니크 제약이 담당)
     */
    @Transactional(readOnly = true)
    public String generateUniqueNickname() {
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String candidate = nicknameGenerator.generate();
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        validateNickname(nickname);
        if (userRepository.existsByNickname(nickname)) {
            throw new CommaException(ErrorCode.DUPLICATE_NICKNAME);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommaException(ErrorCode.USER_NOT_FOUND));
        user.completeOnboarding(nickname);

        // 사전 조회를 통과했더라도 동시에 같은 닉네임이 저장되면 유니크 제약이 막는다.
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CommaException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateNickname(String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new CommaException(ErrorCode.INVALID_NICKNAME);
        }
    }
}