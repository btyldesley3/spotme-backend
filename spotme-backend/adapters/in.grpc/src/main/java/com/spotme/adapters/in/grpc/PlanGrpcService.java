package com.spotme.adapters.in.grpc;

import com.spotme.application.usecase.ComputeNextPrescription;
import com.spotme.proto.plan.v1.PlanServiceGrpc;
import com.spotme.proto.plan.v1.RecommendRequest;
import com.spotme.proto.plan.v1.RecommendResponse;
import com.spotme.proto.plan.v1.SetPrescription;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class PlanGrpcService extends PlanServiceGrpc.PlanServiceImplBase {

    private final ComputeNextPrescription useCase;

    public PlanGrpcService(ComputeNextPrescription useCase) {
        this.useCase = useCase;
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
}
