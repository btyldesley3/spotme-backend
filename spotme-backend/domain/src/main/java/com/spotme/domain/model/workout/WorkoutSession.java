package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.rules.ProgressionInput;
import com.spotme.domain.rules.ProgressionPolicy;
import com.spotme.domain.rules.RecoveryAssessment;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root representing a single completed or in-progress workout session.
 */
public class WorkoutSession {

    private final WorkoutSessionId sessionId;
    private final UserId userId;
    private final Instant startedAt;
    private Instant finishedAt;

    private final Map<ExerciseId, List<SetEntry>> setsByExercise = new LinkedHashMap<>();
    private Doms domsReport;
    private SleepQuality sleepQualityReport;
    private WorkoutCompletionPolicy completionPolicy;

    public WorkoutSession(WorkoutSessionId sessionId, UserId userId, Instant startedAt) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    public static WorkoutSession start(UserId userId, Instant startedAt) {
        return new WorkoutSession(WorkoutSessionId.random(), userId, startedAt);
    }

    public void addSet(ExerciseId exId, SetEntry set) {
        ensureNotFinished();
        Objects.requireNonNull(exId, "exerciseId");
        Objects.requireNonNull(set, "set");

        var existingSets = setsByExercise.computeIfAbsent(exId, ignored -> new ArrayList<>());
        int expectedSetNumber = existingSets.size() + 1;
        if (set.setNumber() != expectedSetNumber) {
            throw new IllegalArgumentException("Set numbers must be sequential per exercise. Expected " + expectedSetNumber + " but got " + set.setNumber());
        }

        existingSets.add(set);
    }

    public void finish(Instant when) {
        finish(when, WorkoutCompletionPolicy.permissive());
    }

    public void finish(Instant when, WorkoutCompletionPolicy completionPolicy) {
        ensureNotFinished();
        Objects.requireNonNull(when, "when");
        this.completionPolicy = Objects.requireNonNull(completionPolicy, "completionPolicy");
        if (setsByExercise.isEmpty()) {
            throw new IllegalStateException("Cannot finish a workout session with no recorded sets");
        }
        if (when.isBefore(startedAt)) {
            throw new IllegalArgumentException("Workout session cannot finish before it starts");
        }
        this.finishedAt = when;
    }

    public void reportDoms(Doms doms) {
        ensureFinished();
        this.domsReport = Objects.requireNonNull(doms, "doms");
    }

    public void reportSleepQuality(SleepQuality sleepQuality) {
        ensureFinished();
        this.sleepQualityReport = Objects.requireNonNull(sleepQuality, "sleepQuality");
    }

    public void reportRecovery(Doms doms, SleepQuality sleepQuality) {
        reportDoms(doms);
        reportSleepQuality(sleepQuality);
    }

    public Optional<ProgressionInput> progressionInputFor(ExerciseId exerciseId) {
        Objects.requireNonNull(exerciseId, "exerciseId");
        var sets = setsByExercise.get(exerciseId);
        if (!completionDecision().allowsProgression()) {
            return Optional.empty();
        }
        if (sets == null || sets.isEmpty() || domsReport == null || sleepQualityReport == null) {
            return Optional.empty();
        }

        var topSet = sets.stream()
                .max(SetEntry.topSetComparator())
                .orElseThrow();

        return Optional.of(new ProgressionInput(
                topSet.weightKg(),
                topSet.reps(),
                topSet.rpe(),
                domsReport,
                sleepQualityReport
        ));
    }

    public boolean containsExercise(ExerciseId exerciseId) {
        return setsByExercise.containsKey(exerciseId);
    }

    public WorkoutCompletionDecision completionDecision() {
        if (finishedAt == null) {
            return WorkoutCompletionDecision.inProgress();
        }

        WorkoutCompletionPolicy policy = completionPolicy == null
                ? WorkoutCompletionPolicy.permissive()
                : completionPolicy;

        boolean hasRecoveryFeedback = domsReport != null && sleepQualityReport != null;
        return policy.evaluate(setsByExercise, hasRecoveryFeedback);
    }

    public WorkoutSessionSummary summary() {
        return summary(null);
    }

    public WorkoutSessionSummary summary(ProgressionPolicy progressionPolicy) {
        List<ExerciseSessionSummary> exerciseSummaries = setsByExercise.entrySet().stream()
                .map(entry -> toExerciseSummary(entry.getKey(), entry.getValue()))
                .toList();

        int totalSets = exerciseSummaries.stream().mapToInt(ExerciseSessionSummary::totalSets).sum();
        int totalReps = exerciseSummaries.stream().mapToInt(ExerciseSessionSummary::totalReps).sum();
        double totalVolume = exerciseSummaries.stream().mapToDouble(ExerciseSessionSummary::totalVolumeKg).sum();

        Optional<RecoveryAssessment.LoadAdjustmentSignal> signal = Optional.empty();
        if (progressionPolicy != null && domsReport != null && sleepQualityReport != null && !exerciseSummaries.isEmpty()) {
            var globalTopSet = exerciseSummaries.stream()
                    .map(ExerciseSessionSummary::topSet)
                    .max(SetEntry.topSetComparator());
            if (globalTopSet.isPresent()) {
                var assessment = new RecoveryAssessment(
                        domsReport,
                        globalTopSet.get().rpe(),
                        sleepQualityReport,
                        progressionPolicy
                );
                signal = Optional.of(assessment.getSignal());
            }
        }

        return new WorkoutSessionSummary(
                sessionId,
                userId,
                startedAt,
                Optional.ofNullable(finishedAt),
                exerciseSummaries.size(),
                totalSets,
                totalReps,
                totalVolume,
                completionDecision(),
                signal,
                exerciseSummaries
        );
    }

    private ExerciseSessionSummary toExerciseSummary(ExerciseId exerciseId, List<SetEntry> sets) {
        int totalSets = sets.size();
        int totalReps = sets.stream().mapToInt(SetEntry::reps).sum();
        double totalVolumeKg = sets.stream().mapToDouble(set -> set.reps() * set.weightKg()).sum();
        SetEntry topSet = sets.stream().max(SetEntry.topSetComparator()).orElseThrow();
        return new ExerciseSessionSummary(exerciseId, totalSets, totalReps, totalVolumeKg, topSet);
    }

    private void ensureNotFinished() {
        if (finishedAt != null) {
            throw new IllegalStateException("Workout session is already finished");
        }
    }

    private void ensureFinished() {
        if (finishedAt == null) {
            throw new IllegalStateException("Workout session must be finished before recovery can be reported");
        }
    }

    // Accessors

    public WorkoutSessionId sessionId() {
        return sessionId;
    }

    public UserId userId() {
        return userId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<Doms> doms() {
        return Optional.ofNullable(domsReport);
    }

    public Optional<SleepQuality> sleepQuality() {
        return Optional.ofNullable(sleepQualityReport);
    }

    public Optional<WorkoutCompletionPolicy> completionPolicy() {
        return Optional.ofNullable(completionPolicy);
    }

    public Map<ExerciseId, List<SetEntry>> sets() {
        Map<ExerciseId, List<SetEntry>> copy = new LinkedHashMap<>();
        setsByExercise.forEach((exerciseId, entries) -> copy.put(exerciseId, List.copyOf(entries)));
        return Collections.unmodifiableMap(copy);
    }
}
