package com.spotme.adapters.in.grpc;

import io.grpc.Context;

import java.util.Optional;

/**
 * Carries authenticated identity through the gRPC call context.
 */
public final class GrpcAuthenticationContext {

    static final Context.Key<String> AUTHENTICATED_USER_ID = Context.key("spotme.authenticatedUserId");

    private GrpcAuthenticationContext() {
    }

    public static Optional<String> currentUserId() {
        return Optional.ofNullable(AUTHENTICATED_USER_ID.get());
    }
}

