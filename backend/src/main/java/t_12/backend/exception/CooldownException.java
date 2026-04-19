package t_12.backend.exception;

/**
 * Thrown when a wallet attempts to mine before its cooldown period has elapsed.
 * retryAfter indicates how many seconds the client should wait before retrying.
 */
public class CooldownException extends RuntimeException {

    private final long retryAfter;

    public CooldownException(String message, long retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public long getRetryAfter() {
        return retryAfter;
    }
}
