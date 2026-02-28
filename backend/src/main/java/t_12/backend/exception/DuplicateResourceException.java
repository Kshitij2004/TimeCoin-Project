package t_12.backend.exception;

// A dedicated exception for when a user tries to register
// with a username or email that already exists.
// Using a separate exception class means we can return a different
// HTTP status code (409 Conflict) instead of the generic 404.
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}