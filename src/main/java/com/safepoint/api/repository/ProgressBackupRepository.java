package com.safepoint.api.repository;

import com.safepoint.api.entity.ProgressBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgressBackupRepository extends JpaRepository<ProgressBackup, Long> {

  Optional<ProgressBackup> findByUserHash(String userHash);

  boolean existsByUserHash(String userHash);

  void deleteByUserHash(String userHash);
}
