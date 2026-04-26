package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.RefreshTokenEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, Instant now);

    @Modifying
    @Transactional
    @Query("update RefreshTokenEntity t set t.revoked = true where t.userId = :userId")
    void revokeAllByUserId(@Param("userId") UUID userId);
}

