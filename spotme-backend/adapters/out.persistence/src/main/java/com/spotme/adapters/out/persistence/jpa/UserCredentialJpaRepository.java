package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredentialEntity, UUID> {
    Optional<UserCredentialEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}

