package com.spotme.adapters.in.rest;

import com.spotme.proto.plan.v1.CompleteWorkoutSessionRequest;
import com.spotme.proto.plan.v1.GetUserProfileRequest;
import com.spotme.proto.plan.v1.GetLatestWorkoutSessionRequest;
import com.spotme.proto.plan.v1.ListRecentWorkoutSessionsRequest;
import com.spotme.proto.plan.v1.LogSetRequest;
import com.spotme.proto.plan.v1.PlanServiceGrpc;
import com.spotme.proto.plan.v1.RecommendRequest;
import com.spotme.proto.plan.v1.StartWorkoutSessionRequest;
import com.spotme.proto.plan.v1.WorkoutSessionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class PlanRestController {

    private final PlanServiceGrpc.PlanServiceBlockingStub planStub;

    public PlanRestController(PlanServiceGrpc.PlanServiceBlockingStub planStub) {
        this.planStub = planStub;
    }

    // ── User Profile ─────────────────────────────────────────────────────────

    /**
     * Fetch own profile. Users may only retrieve their own profile.
     * Requesting another user's profile returns 403 Forbidden.
     */
    @GetMapping("/users/{userId}")
    public UserProfileDto getUser(@PathVariable String userId) {
        enforceOwnership(userId);
        var grpcRes = planStub.getUserProfile(GetUserProfileRequest.newBuilder().setUserId(userId).build());
        return new UserProfileDto(
                grpcRes.getUserId(),
                grpcRes.getExperienceLevel(),
                grpcRes.getTrainingGoal(),
                grpcRes.getBaselineSleepHours(),
                grpcRes.getStressSensitivity()
        );
    }

    // ── Workout Sessions ──────────────────────────────────────────────────────

    @PostMapping("/workout-sessions/start")
    public StartWorkoutSessionDto start(@RequestBody(required = false) StartWorkoutSessionRequestDto body) {
        var userId = authenticatedUserId();
        var startedAt = body != null && body.startedAt() != null ? body.startedAt() : "";
        var grpcReq = StartWorkoutSessionRequest.newBuilder()
                .setUserId(userId)
                .setStartedAt(startedAt)
                .build();

        var grpcRes = planStub.startWorkoutSession(grpcReq);
        return new StartWorkoutSessionDto(grpcRes.getSessionId(), grpcRes.getUserId(), grpcRes.getStartedAt());
    }

    @PostMapping("/workout-sessions/{sessionId}/sets")
    public LogSetDto logSet(@PathVariable String sessionId, @Valid @RequestBody LogSetRequestDto body) {
        var userId = authenticatedUserId();
        var grpcReq = LogSetRequest.newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setExerciseId(body.exerciseId())
                .setSetNumber(body.setNumber())
                .setReps(body.reps())
                .setWeightKg(body.weightKg())
                .setRpe(body.rpe())
                .setNote(body.note() == null ? "" : body.note())
                .build();

        var grpcRes = planStub.logSet(grpcReq);
        return new LogSetDto(grpcRes.getSessionId(), grpcRes.getTotalSetsInSession());
    }

    @PostMapping("/workout-sessions/{sessionId}/complete")
    public CompleteWorkoutDto complete(@PathVariable String sessionId, @Valid @RequestBody CompleteWorkoutRequestDto body) {
        var userId = authenticatedUserId();
        var b = CompleteWorkoutSessionRequest.newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setFinishedAt(body.finishedAt())
                .setMinTotalSets(body.minTotalSets())
                .setMinDistinctExercises(body.minDistinctExercises())
                .setMinSetsPerExercise(body.minSetsPerExercise())
                .setRequireRecoveryFeedbackForProgression(body.requireRecoveryFeedbackForProgression());

        if (body.doms() != null) b.setDoms(body.doms());
        if (body.sleepQuality() != null) b.setSleepQuality(body.sleepQuality());

        var grpcRes = planStub.completeWorkoutSession(b.build());

        return new CompleteWorkoutDto(
                grpcRes.getSessionId(),
                grpcRes.getCompleted(),
                grpcRes.getAllowsProgression(),
                grpcRes.getCompletionReason(),
                grpcRes.getTotalExercises(),
                grpcRes.getTotalSets(),
                grpcRes.getTotalReps(),
                grpcRes.getTotalVolumeKg()
        );
    }

    @GetMapping("/workout-sessions/latest")
    public WorkoutSessionDto latest() {
        var userId = authenticatedUserId();
        var grpcRes = planStub.getLatestWorkoutSession(
                GetLatestWorkoutSessionRequest.newBuilder().setUserId(userId).build()
        );
        return toWorkoutSessionDto(grpcRes);
    }

    @GetMapping("/workout-sessions/recent")
    public RecentSessionsDto recent(@RequestParam(defaultValue = "10") int limit) {
        var userId = authenticatedUserId();
        var grpcRes = planStub.listRecentWorkoutSessions(
                ListRecentWorkoutSessionsRequest.newBuilder().setUserId(userId).setLimit(limit).build()
        );
        var sessions = grpcRes.getSessionsList().stream().map(this::toWorkoutSessionDto).toList();
        return new RecentSessionsDto(sessions);
    }

    // ── Recommendations ───────────────────────────────────────────────────────

    @PostMapping("/recommendations")
    public RecommendDto recommend(@Valid @RequestBody RecommendRequestDto body) {
        var userId = authenticatedUserId();
        var grpcReq = RecommendRequest.newBuilder()
                .setUserId(userId)
                .setExerciseId(body.exerciseId())
                .setRulesVersion(body.rulesVersion() == null ? "" : body.rulesVersion())
                .setModalityKey(body.modalityKey() == null ? "" : body.modalityKey())
                .build();

        var grpcRes = planStub.recommend(grpcReq);
        var sets = grpcRes.getSetsList().stream()
                .map(s -> new PrescriptionSetDto(
                        s.getExerciseId(),
                        s.getOrder(),
                        s.getPrescribedReps(),
                        s.getPrescribedWeightKg(),
                        s.getIsBackoff()))
                .toList();

        return new RecommendDto(sets);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the authenticated user's id from the JWT principal set by {@link com.spotme.adapters.in.rest.security.JwtAuthFilter}.
     * This is always safe to call on protected routes; Spring Security guarantees the principal is set.
     */
    private String authenticatedUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Asserts that the path/query-param userId matches the JWT principal.
     * Prevents a logged-in user from accessing another user's resources.
     *
     * @throws ResponseStatusException 403 if ownership check fails.
     */
    private void enforceOwnership(String resourceUserId) {
        if (!authenticatedUserId().equals(resourceUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorised to access resources belonging to another user");
        }
    }

    private WorkoutSessionDto toWorkoutSessionDto(WorkoutSessionResponse s) {
        return new WorkoutSessionDto(
                s.getSessionId(),
                s.getUserId(),
                s.getStartedAt(),
                s.getFinishedAt().isBlank() ? null : s.getFinishedAt(),
                s.getTotalExercises(),
                s.getTotalSets(),
                s.getTotalReps(),
                s.getTotalVolumeKg(),
                s.getCompleted(),
                s.getAllowsProgression(),
                s.getCompletionReason()
        );
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    /** userId is omitted — derived from JWT on the server side. */
    public record StartWorkoutSessionRequestDto(String startedAt) {}
    public record StartWorkoutSessionDto(String sessionId, String userId, String startedAt) {}

    public record UserProfileDto(
            String userId,
            String experienceLevel,
            String trainingGoal,
            int baselineSleepHours,
            int stressSensitivity
    ) {}

    /** userId is omitted — derived from JWT on the server side. */
    public record LogSetRequestDto(
            @NotBlank String exerciseId,
            @Positive int setNumber,
            @Positive int reps,
            @Positive double weightKg,
            @Min(0) @Max(10) double rpe,
            String note
    ) {}
    public record LogSetDto(String sessionId, int totalSetsInSession) {}

    /** userId is omitted — derived from JWT on the server side. */
    public record RecommendRequestDto(@NotBlank String exerciseId, String rulesVersion, String modalityKey) {}
    public record RecommendDto(List<PrescriptionSetDto> sets) {}
    public record PrescriptionSetDto(String exerciseId, int order, int prescribedReps, double prescribedWeightKg, boolean isBackoff) {}

    /** userId is omitted — derived from JWT on the server side. */
    public record CompleteWorkoutRequestDto(
            @NotBlank String finishedAt,
            @Positive int minTotalSets,
            @Positive int minDistinctExercises,
            @Positive int minSetsPerExercise,
            boolean requireRecoveryFeedbackForProgression,
            Integer doms,
            Integer sleepQuality
    ) {}
    public record CompleteWorkoutDto(
            String sessionId,
            boolean completed,
            boolean allowsProgression,
            String completionReason,
            int totalExercises,
            int totalSets,
            int totalReps,
            double totalVolumeKg
    ) {}

    public record WorkoutSessionDto(
            String sessionId,
            String userId,
            String startedAt,
            String finishedAt,
            int totalExercises,
            int totalSets,
            int totalReps,
            double totalVolumeKg,
            boolean completed,
            boolean allowsProgression,
            String completionReason
    ) {}
    public record RecentSessionsDto(List<WorkoutSessionDto> sessions) {}
}

