package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.InviteCodeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InviteCodeJpaRepository extends JpaRepository<InviteCodeEntity, UUID> {

    @Query("""
        select c from InviteCodeEntity c
        where c.codeHash = :hash
          and c.active = true
          and c.usedCount < c.maxUses
          and (c.expiresAt is null or c.expiresAt > :now)
        """)
    Optional<InviteCodeEntity> findValidByCodeHash(@Param("hash") String hash, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("update InviteCodeEntity c set c.usedCount = c.usedCount + 1 where c.id = :id")
    void incrementUsedCount(@Param("id") UUID id);
}

