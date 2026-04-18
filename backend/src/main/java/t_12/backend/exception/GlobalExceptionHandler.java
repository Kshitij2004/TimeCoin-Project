package t_12.backend.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that converts exceptions thrown by controllers or
 * services into standardized HTTP responses with appropriate status codes and
 * error bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle cases where a requested resource could not be located.
     *
     * @param ex the thrown ResourceNotFoundException
     * @return response with 404 status and JSON error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    /**
     * Fallback for any uncaught exceptions. Returns a generic 500 response
     * without exposing internal details to the client.
     *
     * @param ex the caught exception
     * @return response with 500 status and a safe error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred"));
    }

    /**
     * Handles attempts to create duplicate resources (e.g. duplicate user).
     *
     * @param ex the thrown DuplicateResourceException
     * @return response with 409 status and conflict details
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage()
        ));
    }

    /**
     * Handles request errors where the service already knows the intended HTTP
     * status code.
     *
     * @param ex the thrown ApiException
     * @return response with the exception's status and message
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(
            ApiException ex) {
        HttpStatus status = ex.getStatus();
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", ex.getMessage()));
    }

    /**
     * Handles nonce mismatches with structured expected/provided values.
     *
     * @param ex the thrown InvalidNonceException
     * @return response with 400 status and nonce mismatch details
     */
    @ExceptionHandler(InvalidNonceException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidNonce(
            InvalidNonceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "expectedNonce", ex.getExpectedNonce(),
                "providedNonce", ex.getProvidedNonce()
        ));
    }

    /**
     * Handles forbidden access attempts, such as ownership violations.
     *
     * @param ex the caught ForbiddenException
     * @return response with 403 status and the exception message
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 403,
                        "error", "Forbidden",
                        "message", ex.getMessage()));
    }

    /**
     * Handles transactions rejected due to insufficient sender balance.
     *
     * @param ex the thrown InsufficientFundsException
     * @return response with 400 status and error details
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
            InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()));
    }

    /**
     * Handles mining requests rejected due to an active cooldown period.
     * retryAfter tells the client how many seconds to wait before retrying.
     *
     * @param ex the thrown CooldownException
     * @return response with 429 status and retryAfter value
     */
    @ExceptionHandler(CooldownException.class)
    public ResponseEntity<Map<String, Object>> handleCooldown(CooldownException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 429,
                "error", "Too Many Requests",
                "message", ex.getMessage(),
                "retryAfter", ex.getRetryAfter()
        ));
    }
}
