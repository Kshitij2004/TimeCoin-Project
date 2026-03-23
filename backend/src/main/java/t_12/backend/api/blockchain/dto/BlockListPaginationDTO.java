package t_12.backend.api.blockchain.dto;

/**
 * Pagination metadata for block listing responses.
 */
public class BlockListPaginationDTO {

    private final int page;
    private final int limit;
    private final long total;
    private final int totalPages;

    public BlockListPaginationDTO(int page, int limit, long total, int totalPages) {
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
