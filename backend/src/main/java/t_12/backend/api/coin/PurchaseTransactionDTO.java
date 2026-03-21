package t_12.backend.api.coin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.Transaction;

/**
 * Purchase transaction details returned to the client.
 */
public class PurchaseTransactionDTO {

    private final Integer id;
    private final Integer userId;
    private final String symbol;
    private final String type;
    private final BigDecimal amount;
    private final BigDecimal priceAtTime;
    private final BigDecimal totalUsd;
    private final LocalDateTime timestamp;

    public PurchaseTransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.userId = transaction.getUserId();
        this.symbol = transaction.getSymbol();
        this.type = transaction.getTransactionType() == null
                ? null
                : transaction.getTransactionType().name();
        this.amount = transaction.getAmount();
        this.priceAtTime = transaction.getPriceAtTime();
        this.totalUsd = transaction.getTotalUsd();
        this.timestamp = transaction.getTimestamp();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
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

    public BigDecimal getTotalUsd() {
        return totalUsd;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
