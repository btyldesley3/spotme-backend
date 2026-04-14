package com.spotme.adapters.in.rest;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GrpcExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ApiError> handleGrpc(StatusRuntimeException ex) {
        var code = ex.getStatus().getCode();
        var status = map(code);
        var message = ex.getStatus().getDescription() == null ? "Request failed" : ex.getStatus().getDescription();

        return ResponseEntity.status(status).body(new ApiError(
                Instant.now().toString(),
                status.value(),
                code.name(),
                message
        ));
    }

    private HttpStatus map(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    public record ApiError(String timestamp, int status, String error, String message) {}
}
