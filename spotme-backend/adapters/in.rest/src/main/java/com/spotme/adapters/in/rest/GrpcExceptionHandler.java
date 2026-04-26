package com.spotme.adapters.in.rest;

import com.spotme.application.usecase.LoginUser;
import com.spotme.application.usecase.RegisterWithCredentials;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GrpcExceptionHandler {

    @ExceptionHandler(RegisterWithCredentials.AlphaAccessDeniedException.class)
    public ResponseEntity<ApiError> handleAlphaDenied(RegisterWithCredentials.AlphaAccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "ALPHA_ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(LoginUser.AuthenticationFailedException.class)
    public ResponseEntity<ApiError> handleAuthFailed(LoginUser.AuthenticationFailedException ex) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage());
    }

    @ExceptionHandler(AuthRestController.InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefresh(AuthRestController.InvalidRefreshTokenException ex) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        return error(status, status.name(), ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("validation failed");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ApiError> handleGrpc(StatusRuntimeException ex) {
        var code = ex.getStatus().getCode();
        var status = map(code);
        var message = ex.getStatus().getDescription() == null ? "Request failed" : ex.getStatus().getDescription();

        return error(status, code.name(), message);
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

    private ResponseEntity<ApiError> error(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now().toString(),
                status.value(),
                error,
                message
        ));
    }

    public record ApiError(String timestamp, int status, String error, String message) {}
}
