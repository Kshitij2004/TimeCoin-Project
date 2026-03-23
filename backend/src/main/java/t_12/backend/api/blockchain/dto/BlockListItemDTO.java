package t_12.backend.api.blockchain.dto;

import java.time.LocalDateTime;

import t_12.backend.entity.Block;

/**
 * Lightweight block summary used in paginated explorer listing.
 */
public class BlockListItemDTO {

    private final Integer blockHeight;
    private final String blockHash;
    private final String previousHash;
    private final LocalDateTime timestamp;
    private final Integer transactionCount;
    private final Block.Status status;
    private final String validatorAddress;

    public BlockListItemDTO(Block block) {
        this.blockHeight = block.getBlockHeight();
        this.blockHash = block.getBlockHash();
        this.previousHash = block.getPreviousHash();
        this.timestamp = block.getTimestamp();
        this.transactionCount = block.getTransactionCount();
        this.status = block.getStatus();
        this.validatorAddress = block.getValidatorAddress();
    }

    public Integer getBlockHeight() {
        return blockHeight;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getPreviousHash() {
        return previousHash;
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

    public String getValidatorAddress() {
        return validatorAddress;
    }
}
