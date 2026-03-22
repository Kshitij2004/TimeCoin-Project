package t_12.backend.api.blockchain.dto;

import java.time.LocalDateTime;
import java.util.List;

import t_12.backend.entity.Block;

/**
 * Block detail response including linked transactions for explorer views.
 */
public class BlockDetailDTO {

    private final Integer id;
    private final Integer blockHeight;
    private final String previousHash;
    private final String blockHash;
    private final String validatorAddress;
    private final LocalDateTime timestamp;
    private final Integer transactionCount;
    private final Block.Status status;
    private final List<ExplorerTransactionDTO> transactions;

    public BlockDetailDTO(Block block, List<ExplorerTransactionDTO> transactions) {
        this.id = block.getId();
        this.blockHeight = block.getBlockHeight();
        this.previousHash = block.getPreviousHash();
        this.blockHash = block.getBlockHash();
        this.validatorAddress = block.getValidatorAddress();
        this.timestamp = block.getTimestamp();
        this.transactionCount = block.getTransactionCount();
        this.status = block.getStatus();
        this.transactions = transactions;
    }

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

    public Block.Status getStatus() {
        return status;
    }

    public List<ExplorerTransactionDTO> getTransactions() {
        return transactions;
    }
}
