package com.spotme.adapters.in.rest.config;

import com.spotme.proto.plan.v1.PlanServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestToGrpcConfig {

    private static final Key<String> AUTHORIZATION_METADATA =
            Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

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
        return PlanServiceGrpc.newBlockingStub(planChannel)
                .withInterceptors(forwardAuthorizationHeaderInterceptor());
    }

    private ClientInterceptor forwardAuthorizationHeaderInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions,
                    Channel next
            ) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        var authHeader = currentAuthorizationHeader();
                        if (authHeader != null && !authHeader.isBlank()) {
                            headers.put(AUTHORIZATION_METADATA, authHeader);
                        }
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }

    private String currentAuthorizationHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
}

