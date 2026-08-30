package com.igrejahub.plans.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.plans.entity.Plan;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends BaseRepository<Plan, Long> {
    List<Plan> findByActiveTrue();
    Optional<Plan> findByName(String name);
}