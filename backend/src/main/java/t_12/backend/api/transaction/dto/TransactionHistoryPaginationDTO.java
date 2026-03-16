package t_12.backend.api.transaction.dto;

/**
 * Pagination details returned with transaction history.
 */
public class TransactionHistoryPaginationDTO {

    private final int page;
    private final int limit;
    private final long total;
    private final int totalPages;

    /**
     * Creates pagination metadata for a transaction history response.
     *
     * @param page current 1-based page number
     * @param limit maximum number of records per page
     * @param total total matching records across all pages
     * @param totalPages total number of available pages
     */
    public TransactionHistoryPaginationDTO(int page, int limit, long total, int totalPages) {
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
