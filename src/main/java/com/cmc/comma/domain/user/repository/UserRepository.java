package com.cmc.comma.domain.user.repository;

import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByNickname(String nickname);

    long countByLastActiveAtAfter(LocalDateTime lastActiveAt);

    // "1시간 내 접속자 수" 집계용 조건부 UPDATE. 스로틀 판단(스테일 여부)은 호출부에서 이미 끝낸 뒤
    // 부르므로 여기선 그냥 지금 시각으로 덮어쓴다.
    @Modifying
    @Query("update User u set u.lastActiveAt = :now where u.id = :userId")
    void updateLastActiveAt(Long userId, LocalDateTime now);
}
