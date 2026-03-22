package t_12.backend.api.block;

/**
 * Response body returned after a successful block assembly.
 */
public class BlockResponse {

    /** The height of the newly committed block in the chain. */
    private Integer blockHeight;

    /** The SHA-256 hash of the newly committed block. */
    private String blockHash;

    /** The hash of the preceding block this block links to. */
    private String previousHash;

    /** Number of transactions included in this block. */
    private Integer transactionCount;

    /** Status of the block — always COMMITTED on success. */
    private String status;

    public BlockResponse(Integer blockHeight, String blockHash,
                                 String previousHash, Integer transactionCount,
                                 String status) {
        this.blockHeight = blockHeight;
        this.blockHash = blockHash;
        this.previousHash = previousHash;
        this.transactionCount = transactionCount;
        this.status = status;
    }

    public Integer getBlockHeight() { return blockHeight; }
    public String getBlockHash() { return blockHash; }
    public String getPreviousHash() { return previousHash; }
    public Integer getTransactionCount() { return transactionCount; }
    public String getStatus() { return status; }
}