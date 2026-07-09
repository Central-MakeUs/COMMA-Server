package com.cmc.comma.domain.activity.repository;

import com.cmc.comma.domain.activity.entity.Activity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    long countByRelaxIdAndStartedAtAfter(Long relaxId, LocalDateTime startedAt);
}