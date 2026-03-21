package t_12.backend.api.coin;

/**
 * Response body returned after a successful purchase.
 */
public class PurchaseResponse {

    private final String message;
    private final PurchaseTransactionDTO transaction;
    private final PurchaseWalletDTO wallet;

    public PurchaseResponse(String message,
            PurchaseTransactionDTO transaction,
            PurchaseWalletDTO wallet) {
        this.message = message;
        this.transaction = transaction;
        this.wallet = wallet;
    }

    public String getMessage() {
        return message;
    }

    public PurchaseTransactionDTO getTransaction() {
        return transaction;
    }

    public PurchaseWalletDTO getWallet() {
        return wallet;
    }
}
