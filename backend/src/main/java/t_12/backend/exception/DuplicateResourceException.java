package t_12.backend.exception;

/**
 * Thrown when an attempt is made to create a resource that already exists (e.g.
 * registering with a username or email that is already taken). Using a
 * dedicated exception allows the controller advice to return a 409 Conflict
 * status instead of a generic error.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
