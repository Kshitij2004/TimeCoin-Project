package t_12.backend.exception;

/**
 * Raised when a unique resource such as a username or email already exists.
 */
public class DuplicateResourceException extends RuntimeException {
    /**
     * Creates an exception with a message describing the duplicate resource.
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
