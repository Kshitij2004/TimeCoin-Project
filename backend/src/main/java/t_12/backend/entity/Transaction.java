package t_12.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A single TimeCoin transfer between wallet addresses. Uses addresses
// (not user IDs) because on-chain identity is the wallet address.
// Lifecycle: PENDING (in mempool) -> CONFIRMED (in a block) or REJECTED.
@Entity
@Table(name = "transactions")
public class Transaction {

    public enum TransactionType {
        BUY, SELL, TRANSFER, DEPOSIT, WITHDRAWAL, MINT
    }

    public enum Status {
        PENDING, CONFIRMED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // nullable to allow coinbase/reward transactions that mint new TimeCoin
    @Column(name = "sender_address", length = 128)
    private String senderAddress;

    @Column(name = "receiver_address", nullable = false, length = 128)
    private String receiverAddress;

    @Column(name = "amount", nullable = false, precision = 18, scale = 8)
    private BigDecimal amount;

    // Present for purchase history records; null for pure blockchain transfers.
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "symbol", length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "price_at_time", precision = 15, scale = 2)
    private BigDecimal priceAtTime;

    @Column(name = "total_usd", precision = 18, scale = 2)
    private BigDecimal totalUsd;

    // paid by sender to incentivize validators
    @Column(name = "fee", nullable = false, precision = 18, scale = 8)
    private BigDecimal fee;

    // per-sender sequence number to prevent replay attacks
    @Column(name = "nonce", nullable = false)
    private Integer nonce;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    // SHA-256 over canonical fields; deterministic and tamper-evident
    @Column(name = "transaction_hash", nullable = false, unique = true, length = 128)
    private String transactionHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // null while sitting in the mempool, set once included in a committed block
    @Column(name = "block_id")
    private Integer blockId;

    // getters and setters

    public Integer getId() {
        return id;
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

    public Integer getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getPriceAtTime() {
        return priceAtTime;
    }

    public BigDecimal getTotalUsd() {
        return totalUsd;
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

    public String getTransactionHash() {
        return transactionHash;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public void setPriceAtTime(BigDecimal priceAtTime) {
        this.priceAtTime = priceAtTime;
    }

    public void setTotalUsd(BigDecimal totalUsd) {
        this.totalUsd = totalUsd;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public void setNonce(Integer nonce) {
        this.nonce = nonce;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }
}
