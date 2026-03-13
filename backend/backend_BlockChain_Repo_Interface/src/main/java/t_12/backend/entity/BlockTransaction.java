package t_12.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Join table entity linking a block to the transactions it contains.
 */
@Entity
@Table(name = "block_transactions")
public class BlockTransaction {

    // Primary key for the join row.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Referenced block id.
    @Column(name = "block_id", nullable = false)
    private Integer blockId;

    // Referenced transaction id.
    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;

    /**
     * Returns the join row id.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the join row id.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }
}
