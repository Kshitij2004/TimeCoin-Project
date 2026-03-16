package t_12.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Join table linking blocks to their transactions. Exists alongside
// transactions.block_id so we can efficiently query "all txs in block X"
// without scanning the full transactions table.
@Entity
@Table(name = "block_transactions")
public class BlockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "block_id", nullable = false)
    private Integer blockId;

    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;

    // getters and setters

    public Integer getId() {
        return id;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }
}