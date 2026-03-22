package t_12.backend.api.blockchain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.Transaction;

/**
 * Transaction payload used by blockchain explorer block detail responses.
 */
public class ExplorerTransactionDTO {

    private final Integer id;
    private final String transactionHash;
    private final String senderAddress;
    private final String receiverAddress;
    private final BigDecimal amount;
    private final BigDecimal fee;
    private final Integer nonce;
    private final LocalDateTime timestamp;
    private final Transaction.Status status;

    public ExplorerTransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.transactionHash = transaction.getTransactionHash();
        this.senderAddress = transaction.getSenderAddress();
        this.receiverAddress = transaction.getReceiverAddress();
        this.amount = transaction.getAmount();
        this.fee = transaction.getFee();
        this.nonce = transaction.getNonce();
        this.timestamp = transaction.getTimestamp();
        this.status = transaction.getStatus();
    }

    public Integer getId() {
        return id;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public Integer getNonce() {
        return nonce;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Transaction.Status getStatus() {
        return status;
    }
}
