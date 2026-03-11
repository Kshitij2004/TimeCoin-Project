package t_12.backend.exception;

/**
 * Indicates that a requested resource could not be found. Typically used by
 * services when a lookup by ID or other key returns no result.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
