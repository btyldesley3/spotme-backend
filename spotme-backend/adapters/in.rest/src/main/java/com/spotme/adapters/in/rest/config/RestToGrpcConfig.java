package com.spotme.adapters.in.rest.config;

import com.spotme.proto.plan.v1.PlanServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestToGrpcConfig {

    @Value("${spotme.grpc.client.host:localhost}")
    private String grpcHost;

    @Value("${spotme.grpc.client.port:9090}")
    private int grpcPort;

    @Value("${spotme.grpc.client.plaintext:true}")
    private boolean plaintext;

    @Bean(destroyMethod = "shutdown")
    ManagedChannel planChannel() {
        var builder = ManagedChannelBuilder.forAddress(grpcHost, grpcPort);
        if (plaintext) {
            builder.usePlaintext();
        }
        return builder.build();
    }

    @Bean
    PlanServiceGrpc.PlanServiceBlockingStub planStub(ManagedChannel planChannel) {
        return PlanServiceGrpc.newBlockingStub(planChannel);
    }
}

