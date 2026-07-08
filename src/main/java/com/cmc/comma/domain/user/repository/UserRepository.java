package com.cmc.comma.domain.user.repository;

import com.cmc.comma.domain.user.entity.Provider;
import com.cmc.comma.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByNickname(String nickname);
}