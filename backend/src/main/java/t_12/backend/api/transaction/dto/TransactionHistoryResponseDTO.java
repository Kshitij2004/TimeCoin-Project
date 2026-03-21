package t_12.backend.api.transaction.dto;

import java.util.List;

/**
 * Paginated transaction history response.
 */
public class TransactionHistoryResponseDTO {

    private final List<TransactionHistoryItemDTO> data;
    private final TransactionHistoryPaginationDTO pagination;

    /**
     * Creates a paginated transaction history response body.
     *
     * @param data transaction rows for the current page
     * @param pagination pagination metadata for the current query
     */
    public TransactionHistoryResponseDTO(
            List<TransactionHistoryItemDTO> data,
            TransactionHistoryPaginationDTO pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<TransactionHistoryItemDTO> getData() {
        return data;
    }

    public TransactionHistoryPaginationDTO getPagination() {
        return pagination;
    }
}
