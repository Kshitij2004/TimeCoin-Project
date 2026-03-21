package t_12.backend.exception;

/**
 * Thrown when a transaction sender does not have sufficient funds to cover the
 * amount and fee.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
