package com.spotme.adapters.in.grpc;

import com.spotme.application.usecase.CompleteWorkoutSession;
import com.spotme.application.usecase.ComputeNextPrescription;
import com.spotme.application.usecase.GetLatestWorkoutSession;
import com.spotme.application.usecase.ListRecentWorkoutSessions;
import com.spotme.domain.model.workout.WorkoutSessionSummary;
import com.spotme.proto.plan.v1.CompleteWorkoutSessionRequest;
import com.spotme.proto.plan.v1.CompleteWorkoutSessionResponse;
import com.spotme.proto.plan.v1.GetLatestWorkoutSessionRequest;
import com.spotme.proto.plan.v1.ListRecentWorkoutSessionsRequest;
import com.spotme.proto.plan.v1.ListWorkoutSessionsResponse;
import com.spotme.proto.plan.v1.PlanServiceGrpc;
import com.spotme.proto.plan.v1.RecommendRequest;
import com.spotme.proto.plan.v1.RecommendResponse;
import com.spotme.proto.plan.v1.SetPrescription;
import com.spotme.proto.plan.v1.WorkoutSessionResponse;
import io.grpc.Status;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.NoSuchElementException;

@GrpcService
public class PlanGrpcService extends PlanServiceGrpc.PlanServiceImplBase {

    private final ComputeNextPrescription useCase;
    private final CompleteWorkoutSession completeWorkoutSession;
    private final GetLatestWorkoutSession getLatestWorkoutSession;
    private final ListRecentWorkoutSessions listRecentWorkoutSessions;

    public PlanGrpcService(ComputeNextPrescription useCase,
                           CompleteWorkoutSession completeWorkoutSession,
                           GetLatestWorkoutSession getLatestWorkoutSession,
                           ListRecentWorkoutSessions listRecentWorkoutSessions) {
        this.useCase = useCase;
        this.completeWorkoutSession = completeWorkoutSession;
        this.getLatestWorkoutSession = getLatestWorkoutSession;
        this.listRecentWorkoutSessions = listRecentWorkoutSessions;
    }

    @Override
    public void recommend(RecommendRequest request,
                          io.grpc.stub.StreamObserver<RecommendResponse> resp) {
        var comNextPres = useCase.handle(new ComputeNextPrescription.Command(
                request.getUserId(),              // UUID string
                request.getExerciseId(),         // UUID string
                request.getRulesVersion().isBlank() ? "v1.0.0" : request.getRulesVersion(),
                request.getModalityKey().isBlank() ? "barbell_upper" : request.getModalityKey()
        ));

        var recResBuilder = RecommendResponse.newBuilder();
        comNextPres.prescription().sets().forEach(s ->
                recResBuilder.addSets(SetPrescription.newBuilder()
                        .setExerciseId(s.exerciseId().toString())   // back to string
                        .setOrder(s.order())
                        .setPrescribedReps(s.prescribedReps())
                        .setPrescribedWeightKg(s.prescribedWeightKg())
                        .setIsBackoff(s.backoff())
                        .build())
        );
        resp.onNext(recResBuilder.build());
        resp.onCompleted();
    }

    @Override
    public void completeWorkoutSession(CompleteWorkoutSessionRequest request,
                                       io.grpc.stub.StreamObserver<CompleteWorkoutSessionResponse> resp) {
        try {
            var result = completeWorkoutSession.handle(new CompleteWorkoutSession.Command(
                    request.getUserId(),
                    request.getSessionId(),
                    request.getFinishedAt(),
                    request.getMinTotalSets(),
                    request.getMinDistinctExercises(),
                    request.getMinSetsPerExercise(),
                    request.getRequireRecoveryFeedbackForProgression(),
                    request.hasDoms() ? request.getDoms() : null,
                    request.hasSleepQuality() ? request.getSleepQuality() : null
            ));

            var summary = result.summary();
            var response = CompleteWorkoutSessionResponse.newBuilder()
                    .setSessionId(summary.sessionId().toString())
                    .setCompleted(summary.completionDecision().completed())
                    .setAllowsProgression(summary.completionDecision().allowsProgression())
                    .setCompletionReason(summary.completionDecision().reason().name())
                    .setTotalExercises(summary.totalExercises())
                    .setTotalSets(summary.totalSets())
                    .setTotalReps(summary.totalReps())
                    .setTotalVolumeKg(summary.totalVolumeKg())
                    .build();
            resp.onNext(response);
            resp.onCompleted();
        } catch (NoSuchElementException e) {
            resp.onError(Status.NOT_FOUND.withDescription("Workout session not found").asRuntimeException());
        } catch (IllegalArgumentException | IllegalStateException e) {
            resp.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getLatestWorkoutSession(GetLatestWorkoutSessionRequest request,
                                        io.grpc.stub.StreamObserver<WorkoutSessionResponse> resp) {
        try {
            var result = getLatestWorkoutSession.handle(new GetLatestWorkoutSession.Command(request.getUserId()));
            resp.onNext(toWorkoutSessionResponse(result.summary()));
            resp.onCompleted();
        } catch (NoSuchElementException e) {
            resp.onError(Status.NOT_FOUND.withDescription("Workout session not found").asRuntimeException());
        } catch (IllegalArgumentException e) {
            resp.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listRecentWorkoutSessions(ListRecentWorkoutSessionsRequest request,
                                          io.grpc.stub.StreamObserver<ListWorkoutSessionsResponse> resp) {
        try {
            var result = listRecentWorkoutSessions.handle(
                    new ListRecentWorkoutSessions.Command(request.getUserId(), request.getLimit())
            );

            var response = ListWorkoutSessionsResponse.newBuilder();
            result.sessions().forEach(session -> response.addSessions(toWorkoutSessionResponse(session)));
            resp.onNext(response.build());
            resp.onCompleted();
        } catch (IllegalArgumentException e) {
            resp.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private WorkoutSessionResponse toWorkoutSessionResponse(WorkoutSessionSummary summary) {
        var response = WorkoutSessionResponse.newBuilder()
                .setSessionId(summary.sessionId().toString())
                .setUserId(summary.userId().toString())
                .setStartedAt(summary.startedAt().toString())
                .setTotalExercises(summary.totalExercises())
                .setTotalSets(summary.totalSets())
                .setTotalReps(summary.totalReps())
                .setTotalVolumeKg(summary.totalVolumeKg())
                .setCompleted(summary.completionDecision().completed())
                .setAllowsProgression(summary.completionDecision().allowsProgression())
                .setCompletionReason(summary.completionDecision().reason().name());
        summary.finishedAt().ifPresent(finishedAt -> response.setFinishedAt(finishedAt.toString()));
        return response.build();
    }
}
