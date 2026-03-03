package com.safepoint.api.repository;

import com.safepoint.api.model.entity.WellbeingResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WellbeingResourceRepository extends JpaRepository<WellbeingResource, Long> {

  List<WellbeingResource> findAllByOrderByCategoryAscTitleAsc();

  /** Resources that have no excerpt or an excerpt older than the given cutoff. */
  @Query("SELECT r FROM WellbeingResource r WHERE r.excerpt IS NULL OR r.excerptUpdatedAt < :cutoff")
  List<WellbeingResource> findStaleOrMissingExcerpts(LocalDateTime cutoff);
}
