package t_12.backend.api.marketplace;

/**
 * Response body returned after a successful listing purchase.
 *
 * Example response:
 * {
 *   "transactionHash": "a3f2c1...",
 *   "message": "Purchase successful"
 * }
 */
public class PurchaseResponse {

    /** The SHA-256 hash of the created transaction, used for chain tracking. */
    private String transactionHash;

    /** A human-readable status message. */
    private String message;

    public PurchaseResponse(String transactionHash, String message) {
        this.transactionHash = transactionHash;
        this.message = message;
    }

    public String getTransactionHash() { return transactionHash; }
    public String getMessage() { return message; }
}