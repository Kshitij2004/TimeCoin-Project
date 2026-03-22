package t_12.backend.api.blockchain.dto;

import java.time.LocalDateTime;

/**
 * Chain summary response used by explorer status panels.
 */
public class ChainStatusDTO {

    private final long totalBlocks;
    private final long committedBlocks;
    private final long pendingTransactions;
    private final Integer latestBlockHeight;
    private final String latestBlockHash;
    private final LocalDateTime latestBlockTimestamp;

    public ChainStatusDTO(
            long totalBlocks,
            long committedBlocks,
            long pendingTransactions,
            Integer latestBlockHeight,
            String latestBlockHash,
            LocalDateTime latestBlockTimestamp) {
        this.totalBlocks = totalBlocks;
        this.committedBlocks = committedBlocks;
        this.pendingTransactions = pendingTransactions;
        this.latestBlockHeight = latestBlockHeight;
        this.latestBlockHash = latestBlockHash;
        this.latestBlockTimestamp = latestBlockTimestamp;
    }

    public long getTotalBlocks() {
        return totalBlocks;
    }

    public long getCommittedBlocks() {
        return committedBlocks;
    }

    public long getPendingTransactions() {
        return pendingTransactions;
    }

    public Integer getLatestBlockHeight() {
        return latestBlockHeight;
    }

    public String getLatestBlockHash() {
        return latestBlockHash;
    }

    public LocalDateTime getLatestBlockTimestamp() {
        return latestBlockTimestamp;
    }
}
