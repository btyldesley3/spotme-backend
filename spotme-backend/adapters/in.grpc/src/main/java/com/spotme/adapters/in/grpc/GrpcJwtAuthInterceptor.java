package com.spotme.adapters.in.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;

/**
 * Validates JWT auth at the gRPC boundary and enforces user-ownership for user-scoped requests.
 */
@Component
@GrpcGlobalServerInterceptor
public class GrpcJwtAuthInterceptor implements ServerInterceptor {

    private static final String PLAN_SERVICE_PREFIX = "com.spotme.proto.plan.v1.PlanService/";
    private static final String REGISTER_USER_METHOD = PLAN_SERVICE_PREFIX + "RegisterUser";

    private static final Metadata.Key<String> AUTHORIZATION_HEADER =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;

    public GrpcJwtAuthInterceptor(@Value("${spotme.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String method = call.getMethodDescriptor().getFullMethodName();
        if (!requiresAuthentication(method)) {
            return next.startCall(call, headers);
        }

        String header = headers.get(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing Bearer token"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        String token = header.substring(BEARER_PREFIX.length());
        String authenticatedUserId;
        try {
            authenticatedUserId = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (RuntimeException ex) {
            call.close(Status.UNAUTHENTICATED.withDescription("JWT is invalid or has expired"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        Context context = Context.current().withValue(GrpcAuthenticationContext.AUTHENTICATED_USER_ID, authenticatedUserId);
        ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(context, call, headers, next);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                String requestUserId = extractUserId(message);
                if (requestUserId != null && !requestUserId.isBlank() && !authenticatedUserId.equals(requestUserId)) {
                    call.close(Status.PERMISSION_DENIED
                            .withDescription("Token userId does not match request userId"), new Metadata());
                    return;
                }
                super.onMessage(message);
            }
        };
    }

    private boolean requiresAuthentication(String fullMethodName) {
        if (REGISTER_USER_METHOD.equals(fullMethodName)) {
            return false;
        }
        if (fullMethodName.startsWith("grpc.health.v1.Health/")) {
            return false;
        }
        if (fullMethodName.startsWith("grpc.reflection.v1alpha.ServerReflection/")) {
            return false;
        }
        return true;
    }

    private String extractUserId(Object message) {
        try {
            Method method = message.getClass().getMethod("getUserId");
            Object value = method.invoke(message);
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

