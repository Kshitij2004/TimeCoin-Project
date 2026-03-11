package t_12.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocks")
public class Block {

    public enum Status {
        PENDING, COMMITTED, INVALID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "block_height", nullable = false, unique = true)
    private Integer blockHeight;

    @Column(name = "previous_hash", length = 128)
    private String previousHash;

    @Column(name = "block_hash", nullable = false, unique = true, length = 128)
    private String blockHash;

    @Column(name = "validator_address", length = 128)
    private String validatorAddress;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // Getters
    public Integer getId() {
        return id;
    }

    public Integer getBlockHeight() {
        return blockHeight;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getValidatorAddress() {
        return validatorAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public Status getStatus() {
        return status;
    }

    // Setters
    public void setId(Integer id) {
        this.id = id;
    }

    public void setBlockHeight(Integer blockHeight) {
        this.blockHeight = blockHeight;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public void setValidatorAddress(String validatorAddress) {
        this.validatorAddress = validatorAddress;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}