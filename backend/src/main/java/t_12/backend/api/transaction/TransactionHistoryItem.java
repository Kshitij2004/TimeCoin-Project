package t_12.backend.api.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.Transaction;

/**
 * One row in the transaction history response.
 */
public class TransactionHistoryItem {

    private final String type;
    private final BigDecimal amount;
    private final BigDecimal priceAtTime;
    private final LocalDateTime timestamp;

    public TransactionHistoryItem(Transaction transaction) {
        this.type = transaction.getTransactionType() == null
                ? null
                : transaction.getTransactionType().name();
        this.amount = transaction.getAmount();
        this.priceAtTime = transaction.getPriceAtTime();
        this.timestamp = transaction.getTimestamp();
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPriceAtTime() {
        return priceAtTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
