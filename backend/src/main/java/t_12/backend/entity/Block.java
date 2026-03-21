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

// Represents a single block in the TimeCoin blockchain.
// Each block groups validated transactions and links to its predecessor
// via previousHash, forming the chain. Maps to the "blocks" table in init.sql.
@Entity
@Table(name = "blocks")
public class Block {

    // PENDING = proposed, not yet validated by consensus
    // COMMITTED = accepted into the canonical chain, transactions are final
    // INVALID = failed validation (e.g. bad hash linkage), should be ignored
    public enum Status {
        PENDING, COMMITTED, INVALID
    }

    // surrogate key for JPA internals and FKs, not used in blockchain logic
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // sequential chain position; unique constraint prevents forks at the DB level
    @Column(name = "block_height", nullable = false, unique = true)
    private Integer blockHeight;

    // hash of the preceding block; null only for the genesis block
    @Column(name = "previous_hash", length = 128)
    private String previousHash;

    // SHA-256 of this block's contents; uniqueness guarantees no duplicate blocks
    @Column(name = "block_hash", nullable = false, unique = true, length = 128)
    private String blockHash;

    // PoS validator who proposed this block; null for genesis
    @Column(name = "validator_address", length = 128)
    private String validatorAddress;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    // denormalized count to avoid a JOIN when rendering block explorer summaries
    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    // stored as the enum name string ("COMMITTED") instead of ordinal
    // so adding new statuses later won't corrupt existing data
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // getters and setters for all fields

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