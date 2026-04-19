package t_12.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a transaction nonce does not match the next expected value for
 * a sender.
 */
public class InvalidNonceException extends ApiException {

    private final long expectedNonce;
    private final Integer providedNonce;

    public InvalidNonceException(long expectedNonce, Integer providedNonce) {
        super(
                HttpStatus.BAD_REQUEST,
                "Invalid nonce: expected " + expectedNonce + " but received " + providedNonce + "."
        );
        this.expectedNonce = expectedNonce;
        this.providedNonce = providedNonce;
    }

    public long getExpectedNonce() {
        return expectedNonce;
    }

    public Integer getProvidedNonce() {
        return providedNonce;
    }
}
