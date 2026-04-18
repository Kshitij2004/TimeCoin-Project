package t_12.backend.api.blockchain.dto;

/**
 * Details about the first chain validation failure detected.
 */
public class ChainValidationFailureDTO {

    private final Integer blockHeight;
    private final String blockHash;
    private final String code;
    private final String message;
    private final Integer transactionId;
    private final String transactionHash;

    public ChainValidationFailureDTO(
            Integer blockHeight,
            String blockHash,
            String code,
            String message,
            Integer transactionId,
            String transactionHash) {
        this.blockHeight = blockHeight;
        this.blockHash = blockHash;
        this.code = code;
        this.message = message;
        this.transactionId = transactionId;
        this.transactionHash = transactionHash;
    }

    public Integer getBlockHeight() {
        return blockHeight;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public String getTransactionHash() {
        return transactionHash;
    }
}
