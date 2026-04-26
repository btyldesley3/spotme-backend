package com.spotme.adapters.in.grpc;

import io.grpc.Context;

/**
 * Carries authenticated identity through the gRPC call context.
 */
public final class GrpcAuthenticationContext {

    static final Context.Key<String> AUTHENTICATED_USER_ID = Context.key("spotme.authenticatedUserId");

    private GrpcAuthenticationContext() {
    }
}

