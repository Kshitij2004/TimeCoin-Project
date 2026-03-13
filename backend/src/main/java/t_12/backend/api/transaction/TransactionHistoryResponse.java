package t_12.backend.api.transaction;

import java.util.List;

/**
 * Paginated transaction history response.
 */
public class TransactionHistoryResponse {

    private final List<TransactionHistoryItem> data;
    private final PaginationDTO pagination;

    public TransactionHistoryResponse(
            List<TransactionHistoryItem> data,
            PaginationDTO pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<TransactionHistoryItem> getData() {
        return data;
    }

    public PaginationDTO getPagination() {
        return pagination;
    }
}
