package com.spotme.adapters.in.rest;

import com.spotme.proto.plan.v1.CompleteWorkoutSessionRequest;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class PlanRestController {

    private final PlanServiceGrpc.PlanServiceBlockingStub planStub;

    public PlanRestController(PlanServiceGrpc.PlanServiceBlockingStub planStub) {
        this.planStub = planStub;
    }

    @PostMapping("/workout-sessions/start")
    public StartWorkoutSessionDto start(@Valid @RequestBody StartWorkoutSessionRequestDto body) {
        var grpcReq = StartWorkoutSessionRequest.newBuilder()
                .setUserId(body.userId())
                .setStartedAt(body.startedAt() == null ? "" : body.startedAt())
                .build();

        var grpcRes = planStub.startWorkoutSession(grpcReq);
        return new StartWorkoutSessionDto(grpcRes.getSessionId(), grpcRes.getUserId(), grpcRes.getStartedAt());
    }

    @PostMapping("/workout-sessions/{sessionId}/sets")
    public LogSetDto logSet(@PathVariable String sessionId, @Valid @RequestBody LogSetRequestDto body) {
        var grpcReq = LogSetRequest.newBuilder()
                .setUserId(body.userId())
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

    @PostMapping("/recommendations")
    public RecommendDto recommend(@Valid @RequestBody RecommendRequestDto body) {
        var grpcReq = RecommendRequest.newBuilder()
                .setUserId(body.userId())
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

    @PostMapping("/workout-sessions/{sessionId}/complete")
    public CompleteWorkoutDto complete(@PathVariable String sessionId, @Valid @RequestBody CompleteWorkoutRequestDto body) {
        var b = CompleteWorkoutSessionRequest.newBuilder()
                .setUserId(body.userId())
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
    public WorkoutSessionDto latest(@RequestParam String userId) {
        var grpcRes = planStub.getLatestWorkoutSession(
                GetLatestWorkoutSessionRequest.newBuilder().setUserId(userId).build()
        );
        return toWorkoutSessionDto(grpcRes);
    }

    @GetMapping("/workout-sessions/recent")
    public RecentSessionsDto recent(@RequestParam String userId, @RequestParam(defaultValue = "10") int limit) {
        var grpcRes = planStub.listRecentWorkoutSessions(
                ListRecentWorkoutSessionsRequest.newBuilder().setUserId(userId).setLimit(limit).build()
        );
        var sessions = grpcRes.getSessionsList().stream().map(this::toWorkoutSessionDto).toList();
        return new RecentSessionsDto(sessions);
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

    public record StartWorkoutSessionRequestDto(@NotBlank String userId, String startedAt) {}
    public record StartWorkoutSessionDto(String sessionId, String userId, String startedAt) {}

    public record LogSetRequestDto(
            @NotBlank String userId,
            @NotBlank String exerciseId,
            @Positive int setNumber,
            @Positive int reps,
            @Positive double weightKg,
            @Min(0) @Max(10) double rpe,
            String note
    ) {}
    public record LogSetDto(String sessionId, int totalSetsInSession) {}

    public record RecommendRequestDto(@NotBlank String userId, @NotBlank String exerciseId, String rulesVersion, String modalityKey) {}
    public record RecommendDto(List<PrescriptionSetDto> sets) {}
    public record PrescriptionSetDto(String exerciseId, int order, int prescribedReps, double prescribedWeightKg, boolean isBackoff) {}

    public record CompleteWorkoutRequestDto(
            @NotBlank String userId,
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

