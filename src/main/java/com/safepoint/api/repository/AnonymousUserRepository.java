package com.safepoint.api.repository;

import com.safepoint.api.model.entity.AnonymousUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnonymousUserRepository extends JpaRepository<AnonymousUser, Long> {

    Optional<AnonymousUser> findByUserCode(String userCode);

    boolean existsByUserCode(String userCode);
}
