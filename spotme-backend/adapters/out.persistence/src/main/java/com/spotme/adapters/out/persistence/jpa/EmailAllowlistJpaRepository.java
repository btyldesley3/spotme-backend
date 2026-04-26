package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.EmailAllowlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailAllowlistJpaRepository extends JpaRepository<EmailAllowlistEntity, UUID> {
    Optional<EmailAllowlistEntity> findByEmailAndActiveTrue(String email);
}

