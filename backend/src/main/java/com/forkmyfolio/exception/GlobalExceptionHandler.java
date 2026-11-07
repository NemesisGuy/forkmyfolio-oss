package com.forkmyfolio.exception;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.advice.SkipApiResponseWrapper;
import com.forkmyfolio.dto.response.FieldErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.stream.Collectors;

// FIX: Removed the basePackages attribute to make this handler truly global.
// This ensures it applies to all controllers, regardless of their package.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler implements ResponseBodyAdvice<Object> {

    // --- ResponseBodyAdvice implementation (for success wrapping) ---

    /**
     * Determines if this advice should be applied. It will not be applied to
     * methods
     * annotated with @SkipApiResponseWrapper, which is used for file downloads.
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.hasMethodAnnotation(SkipApiResponseWrapper.class);
    }

    /**
     * Wraps successful responses in the standard ApiResponseWrapper before the body
     * is written.
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        // Do not wrap if the body is already our wrapper, a Spring ProblemDetail, or a
        // file download.
        if (body instanceof ApiResponseWrapper || body instanceof ProblemDetail || body instanceof byte[]) {
            return body;
        }

        return new ApiResponseWrapper<>(body);
    }

    // --- ExceptionHandler implementations (for error handling) ---

    /**
     * Handles validation errors for request bodies (@Valid).
     * Returns a standard ApiResponseWrapper with validation details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseWrapper<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<FieldErrorDto> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    log.warn("Validation error on field '{}': {}", fieldName, errorMessage);
                    return new FieldErrorDto(fieldName, errorMessage);
                })
                .collect(Collectors.toList());
        return new ApiResponseWrapper<>(errors, "validation_failed");
    }

    /**
     * Handles malformed request bodies: unparseable JSON, wrong types, and
     * invalid enum values (Phase 8.4: an unknown ApplicationStatus used to
     * fall through to the generic 500 handler — a client error must be 400).
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseWrapper<Object> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("body",
                "Malformed request body: check the JSON syntax and field values."));
        return new ApiResponseWrapper<>(errors, "malformed_request");
    }

    /**
     * Handles validation errors for method parameters (@RequestParam, etc.).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseWrapper<Object> handleMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorDto> errors = ex.getAllValidationResults().stream()
                .flatMap(result -> {
                    String paramName = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors().stream().map(error -> {
                        log.warn("Validation error on parameter '{}': {}", paramName, error.getDefaultMessage());
                        return new FieldErrorDto(paramName, error.getDefaultMessage());
                    });
                })
                .collect(Collectors.toList());
        return new ApiResponseWrapper<>(errors, "validation_failed");
    }

    /**
     * Handles permission denied errors: 403 Forbidden for denials that do
     * NOT conceal a resource's existence — role-based denials (e.g. guest
     * write attempts, /management access) and password re-verification for
     * backup operations.
     *
     * <p>Phase 7.4 (§403/404 policy): CROSS-TENANT access is intentionally
     * NOT handled here — services throw {@link ResourceNotFoundException}
     * instead so a foreign resource is indistinguishable from a nonexistent
     * one (404). Do not reintroduce 403 for "exists but foreign": it
     * enables existence probing.
     */
    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleAuthorizationDenied(
            org.springframework.security.authorization.AuthorizationDeniedException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        ApiResponseWrapper<Object> response = new ApiResponseWrapper<>(
                List.of(new FieldErrorDto("authorization", "You do not have permission to perform this action.")),
                "error");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponseWrapper<Object> handlePermissionDenied(PermissionDeniedException ex) {
        log.warn("Permission denied: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("authorization", ex.getMessage()));
        return new ApiResponseWrapper<>(errors, "permission_denied");
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponseWrapper<Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("parameter", ex.getMessage()));
        return new ApiResponseWrapper<>(errors, "invalid_argument");
    }

    /**
     * Handles attempts to register or create a resource with an email/identifier
     * that already exists (Phase 7.4: duplicates are a client error, not a
     * server error — previously fell through to the generic 500 handler).
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponseWrapper<Object> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("email", ex.getMessage()));
        return new ApiResponseWrapper<>(errors, "email_already_exists");
    }

    /**
     * Handles 404 Not Found errors when a resource is not found.
     * This includes Spring's NoResourceFoundException (Spring Boot 3/Spring 6).
     */
    @ExceptionHandler({
            org.springframework.web.servlet.resource.NoResourceFoundException.class,
            ResourceNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponseWrapper<Object> handleNoResourceFound(Exception ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("resource", ex.getMessage()));
        return new ApiResponseWrapper<>(errors, "resource_not_found");
    }

    /**
     * Handles bad credentials (wrong email or password).
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponseWrapper<Object> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        log.warn("Login failed: Bad credentials");
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("login", "Invalid email or password."));
        return new ApiResponseWrapper<>(errors, "invalid_credentials");
    }

    /**
     * Handles token refresh failures.
     */
    @ExceptionHandler(TokenRefreshException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponseWrapper<Object> handleTokenRefresh(TokenRefreshException ex) {
        log.warn("Token refresh failed: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("refresh_token", ex.getMessage()));
        return new ApiResponseWrapper<>(errors, "token_expired");
    }

    /**
     * DEF-1: analytics reporting cannot be served right now (disabled,
     * unconfigured, missing credentials) — graceful 503 so dashboards show a
     * clear state instead of an error.
     */
    @ExceptionHandler(com.forkmyfolio.exception.AnalyticsUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponseWrapper<Object> handleAnalyticsUnavailable(com.forkmyfolio.exception.AnalyticsUnavailableException ex) {
        log.info("Analytics unavailable: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("analytics", ex.getMessage()));
        return new ApiResponseWrapper<>(errors, "analytics_unavailable");
    }

    /**
     * DEF-1: the analytics provider (e.g. Umami) could not be reached or
     * returned an error — graceful 503, never a 500. The exception messages
     * are curated provider-layer strings (no secrets, no URLs), safe to show.
     */
    @ExceptionHandler(com.forkmyfolio.exception.AnalyticsProviderException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponseWrapper<Object> handleAnalyticsProviderFailure(com.forkmyfolio.exception.AnalyticsProviderException ex) {
        log.warn("Analytics provider failure: {}", ex.getMessage());
        List<FieldErrorDto> errors = List.of(new FieldErrorDto("analytics",
                ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage()
                        : "The analytics provider is currently unreachable. Please try again later."));
        return new ApiResponseWrapper<>(errors, "analytics_provider_unavailable");
    }

    /**
     * A final catch-all for any unexpected exceptions.
     * Returns a generic error message to avoid leaking implementation details.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponseWrapper<Object> handleAllUncaughtException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        List<FieldErrorDto> errors = List
                .of(new FieldErrorDto("general", "An unexpected internal server error occurred."));
        return new ApiResponseWrapper<>(errors, "error");
    }
}