package t_12.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception type for request failures that should map to a specific HTTP
 * status code.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
