package com.cmc.comma.domain.relax.repository;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.entity.Relax;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelaxRepository extends JpaRepository<Relax, Long> {

    List<Relax> findByMoodAndTimeBudget(Mood mood, TimeBudget timeBudget);
}