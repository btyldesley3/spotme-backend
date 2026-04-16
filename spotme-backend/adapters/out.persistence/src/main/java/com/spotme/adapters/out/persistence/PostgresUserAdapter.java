package com.spotme.adapters.out.persistence;

import com.spotme.adapters.out.persistence.jpa.UserJpaRepository;
import com.spotme.adapters.out.persistence.jpa.entity.UserEntity;
import com.spotme.domain.model.program.ProgramId;
import com.spotme.domain.model.user.RecoveryProfile;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.UserWritePort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class PostgresUserAdapter implements UserReadPort, UserWritePort {

    private final UserJpaRepository users;

    public PostgresUserAdapter(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return users.findById(userId.value()).map(this::toDomain);
    }

    @Override
    public void save(User user) {
        users.save(toEntity(user));
    }

    private UserEntity toEntity(User user) {
        var entity = new UserEntity();
        entity.setId(user.id().value());
        entity.setExperienceLevel(user.experienceLevel());
        entity.setTrainingGoal(user.trainingGoal());
        entity.setBaselineSleepHours(user.recoveryProfile().baselineSleepHours());
        entity.setStressSensitivity(user.recoveryProfile().stressSensitivity());
        entity.setActiveProgramId(user.activeProgramId() == null ? null : user.activeProgramId().value());
        return entity;
    }

    private User toDomain(UserEntity entity) {
        var user = new User(
                new UserId(entity.getId()),
                entity.getExperienceLevel(),
                entity.getTrainingGoal(),
                new RecoveryProfile(entity.getBaselineSleepHours(), entity.getStressSensitivity())
        );
        if (entity.getActiveProgramId() != null) {
            user.assignProgram(new ProgramId(entity.getActiveProgramId()));
        }
        return user;
    }
}

