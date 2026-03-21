package t_12.backend.exception;

/**
 * Thrown when an authenticated user attempts to modify a resource they do not
 * own.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
