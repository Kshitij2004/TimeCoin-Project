package t_12.backend.api.blockchain.dto;

/**
 * Chain validation result summary with optional first failure details.
 */
public class ChainValidationReportDTO {

    private final boolean valid;
    private final int checkedBlocks;
    private final int checkedTransactions;
    private final ChainValidationFailureDTO failure;

    public ChainValidationReportDTO(
            boolean valid,
            int checkedBlocks,
            int checkedTransactions,
            ChainValidationFailureDTO failure) {
        this.valid = valid;
        this.checkedBlocks = checkedBlocks;
        this.checkedTransactions = checkedTransactions;
        this.failure = failure;
    }

    public static ChainValidationReportDTO passed(int checkedBlocks, int checkedTransactions) {
        return new ChainValidationReportDTO(true, checkedBlocks, checkedTransactions, null);
    }

    public static ChainValidationReportDTO failed(
            int checkedBlocks,
            int checkedTransactions,
            ChainValidationFailureDTO failure) {
        return new ChainValidationReportDTO(false, checkedBlocks, checkedTransactions, failure);
    }

    public boolean isValid() {
        return valid;
    }

    public int getCheckedBlocks() {
        return checkedBlocks;
    }

    public int getCheckedTransactions() {
        return checkedTransactions;
    }

    public ChainValidationFailureDTO getFailure() {
        return failure;
    }
}
