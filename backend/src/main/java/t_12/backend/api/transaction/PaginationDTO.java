package t_12.backend.api.transaction;

/**
 * Pagination details returned with transaction history.
 */
public class PaginationDTO {

    private final int page;
    private final int limit;
    private final long total;
    private final int totalPages;

    public PaginationDTO(int page, int limit, long total, int totalPages) {
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
