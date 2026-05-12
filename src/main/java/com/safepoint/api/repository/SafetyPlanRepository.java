package com.safepoint.api.repository;

import com.safepoint.api.entity.SafetyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SafetyPlanRepository extends JpaRepository<SafetyPlan, Long> {

    /**
     * Finds a safety plan by the user's anonymous hash.
     */
    Optional<SafetyPlan> findByUserHash(String userHash);

    /**
     * Checks if a safety plan exists for the given hash.
     */
    boolean existsByUserHash(String userHash);

    /**
     * Deletes a safety plan by user hash.
     */
    void deleteByUserHash(String userHash);
}
