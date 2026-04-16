package com.spotme.adapters.out.persistence;

import com.spotme.adapters.out.persistence.jpa.PrescriptionJpaRepository;
import com.spotme.adapters.out.persistence.jpa.WorkoutSessionJpaRepository;
import com.spotme.adapters.out.persistence.jpa.WorkoutSetJpaRepository;
import com.spotme.adapters.out.persistence.jpa.entity.PrescriptionEntity;
import com.spotme.adapters.out.persistence.jpa.entity.WorkoutSessionEntity;
import com.spotme.adapters.out.persistence.jpa.entity.WorkoutSetEntity;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.plan.SetPrescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.SetEntry;
import com.spotme.domain.model.workout.WorkoutCompletionPolicy;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionInput;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class PostgresWorkoutAdapter implements WorkoutReadPort, WorkoutWritePort {

    private final WorkoutSessionJpaRepository sessions;
    private final WorkoutSetJpaRepository sets;
    private final PrescriptionJpaRepository prescriptions;

    public PostgresWorkoutAdapter(WorkoutSessionJpaRepository sessions,
                                  WorkoutSetJpaRepository sets,
                                  PrescriptionJpaRepository prescriptions) {
        this.sessions = sessions;
        this.sets = sets;
        this.prescriptions = prescriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProgressionInput> lastProgressionInput(UserId userId, ExerciseId exerciseId) {
        return sessions.findByUserId(userId.value()).stream()
                .sorted(sessionSortOrder())
                .map(this::toDomain)
                .filter(session -> session.containsExercise(exerciseId))
                .map(session -> session.progressionInputFor(exerciseId))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId) {
        return sessions.findByIdAndUserId(UUID.fromString(sessionId.toString()), userId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkoutSession> findLatestSession(UserId userId) {
        return sessions.findByUserId(userId.value()).stream()
                .max(sessionSortOrder())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
        return sessions.findByUserId(userId.value()).stream()
                .sorted(sessionSortOrder().reversed())
                .limit(limit)
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void savePrescription(UserId userId, Prescription prescription) {
        var entity = new PrescriptionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId.value());
        entity.setCreatedAt(Instant.now());
        entity.setPayloadJson(toJson(prescription));
        prescriptions.save(entity);
    }

    @Override
    public void saveSession(WorkoutSession session) {
        UUID sessionId = UUID.fromString(session.sessionId().toString());
        sessions.saveAndFlush(toSessionEntity(session));
        syncSetEntities(session, sessionId);
    }

    private WorkoutSessionEntity toSessionEntity(WorkoutSession session) {
        var entity = new WorkoutSessionEntity();
        entity.setId(UUID.fromString(session.sessionId().toString()));
        entity.setUserId(session.userId().value());
        entity.setStartedAt(session.startedAt());
        entity.setFinishedAt(session.finishedAt().orElse(null));
        entity.setDoms(session.doms().map(Doms::value).orElse(null));
        entity.setSleepQuality(session.sleepQuality().map(SleepQuality::value).orElse(null));
        entity.setMinTotalSets(session.completionPolicy().map(WorkoutCompletionPolicy::minTotalSets).orElse(null));
        entity.setMinDistinctExercises(session.completionPolicy().map(WorkoutCompletionPolicy::minDistinctExercises).orElse(null));
        entity.setMinSetsPerExercise(session.completionPolicy().map(WorkoutCompletionPolicy::minSetsPerExercise).orElse(null));
        entity.setRequireRecoveryFeedbackForProgression(
                session.completionPolicy().map(WorkoutCompletionPolicy::requireRecoveryFeedbackForProgression).orElse(null)
        );
        return entity;
    }

    private List<WorkoutSetEntity> toSetEntities(WorkoutSession session) {
        var entities = new ArrayList<WorkoutSetEntity>();
        session.sets().forEach((exerciseId, entries) -> entries.forEach(entry -> {
            var entity = new WorkoutSetEntity();
            applySetValues(entity, UUID.fromString(session.sessionId().toString()), exerciseId.value(), entry);
            entities.add(entity);
        }));
        return entities;
    }

    private void syncSetEntities(WorkoutSession session, UUID sessionId) {
        List<WorkoutSetEntity> existing = sets.findBySessionIdOrderByExerciseIdAscSetNumberAsc(sessionId);
        Map<WorkoutSetKey, WorkoutSetEntity> existingByKey = existing.stream()
                .collect(Collectors.toMap(this::toSetKey, entity -> entity));

        List<WorkoutSetEntity> desired = new ArrayList<>();
        Set<WorkoutSetKey> desiredKeys = new HashSet<>();

        session.sets().forEach((exerciseId, entries) -> entries.forEach(entry -> {
            WorkoutSetKey key = new WorkoutSetKey(exerciseId.value(), entry.setNumber());
            desiredKeys.add(key);

            WorkoutSetEntity entity = existingByKey.getOrDefault(key, new WorkoutSetEntity());
            applySetValues(entity, sessionId, exerciseId.value(), entry);
            desired.add(entity);
        }));

        List<WorkoutSetEntity> obsolete = existing.stream()
                .filter(entity -> !desiredKeys.contains(toSetKey(entity)))
                .toList();

        if (!obsolete.isEmpty()) {
            sets.deleteAllInBatch(obsolete);
        }
        if (!desired.isEmpty()) {
            sets.saveAll(desired);
        }
        sets.flush();
    }

    private void applySetValues(WorkoutSetEntity entity, UUID sessionId, UUID exerciseId, SetEntry entry) {
        entity.setSessionId(sessionId);
        entity.setExerciseId(exerciseId);
        entity.setSetNumber(entry.setNumber());
        entity.setReps(entry.reps());
        entity.setWeightKg(entry.weightKg());
        entity.setRpe(entry.rpe().value());
        entity.setNotes(entry.notes());
    }

    private WorkoutSetKey toSetKey(WorkoutSetEntity entity) {
        return new WorkoutSetKey(entity.getExerciseId(), entity.getSetNumber());
    }

    private WorkoutSession toDomain(WorkoutSessionEntity entity) {
        var session = new WorkoutSession(
                WorkoutSessionId.fromString(entity.getId().toString()),
                new UserId(entity.getUserId()),
                entity.getStartedAt()
        );

        Map<UUID, List<WorkoutSetEntity>> groupedSets = sets.findBySessionIdOrderByExerciseIdAscSetNumberAsc(entity.getId()).stream()
                .collect(Collectors.groupingBy(WorkoutSetEntity::getExerciseId));

        groupedSets.forEach((exerciseId, exerciseSets) -> exerciseSets.stream()
                .sorted(Comparator.comparingInt(WorkoutSetEntity::getSetNumber))
                .forEach(set -> session.addSet(new ExerciseId(exerciseId), new SetEntry(
                        set.getSetNumber(),
                        set.getReps(),
                        set.getWeightKg(),
                        new Rpe(set.getRpe()),
                        set.getNotes()
                ))));

        if (entity.getFinishedAt() != null) {
            WorkoutCompletionPolicy completionPolicy = entity.getMinTotalSets() == null
                    ? WorkoutCompletionPolicy.permissive()
                    : new WorkoutCompletionPolicy(
                            entity.getMinTotalSets(),
                            entity.getMinDistinctExercises(),
                            entity.getMinSetsPerExercise(),
                            Boolean.TRUE.equals(entity.getRequireRecoveryFeedbackForProgression())
                    );
            session.finish(entity.getFinishedAt(), completionPolicy);
        }
        if (entity.getDoms() != null && entity.getSleepQuality() != null) {
            session.reportRecovery(new Doms(entity.getDoms()), new SleepQuality(entity.getSleepQuality()));
        }
        return session;
    }

    private Comparator<WorkoutSessionEntity> sessionSortOrder() {
        return Comparator.comparing(this::effectiveSessionTime);
    }

    private Instant effectiveSessionTime(WorkoutSessionEntity session) {
        return session.getFinishedAt() != null ? session.getFinishedAt() : session.getStartedAt();
    }

    private String toJson(Prescription prescription) {
        return prescription.sets().stream()
                .map(this::toJson)
                .collect(Collectors.joining(",", "{\"sets\":[", "]}"));
    }

    private String toJson(SetPrescription set) {
        return "{"
                + "\"exerciseId\":\"" + set.exerciseId() + "\","
                + "\"order\":" + set.order() + ","
                + "\"prescribedReps\":" + set.prescribedReps() + ","
                + "\"prescribedWeightKg\":" + set.prescribedWeightKg() + ","
                + "\"backoff\":" + set.backoff()
                + "}";
    }

    private record WorkoutSetKey(UUID exerciseId, int setNumber) {
    }
}




