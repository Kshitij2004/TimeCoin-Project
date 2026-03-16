package t_12.backend.api.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.Transaction;

/**
 * One row in the transaction history response.
 */
public class TransactionHistoryItemDTO {

    private final String type;
    private final BigDecimal amount;
    private final BigDecimal priceAtTime;
    private final LocalDateTime timestamp;

    /**
     * Maps a transaction entity into a transaction history response row.
     *
     * @param transaction persisted transaction to expose in the response
     */
    public TransactionHistoryItemDTO(Transaction transaction) {
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
