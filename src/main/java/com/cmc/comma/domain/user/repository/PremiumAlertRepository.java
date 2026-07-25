package com.cmc.comma.domain.user.repository;

import com.cmc.comma.domain.user.entity.PremiumAlert;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumAlertRepository extends JpaRepository<PremiumAlert, Long> {

    Optional<PremiumAlert> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
