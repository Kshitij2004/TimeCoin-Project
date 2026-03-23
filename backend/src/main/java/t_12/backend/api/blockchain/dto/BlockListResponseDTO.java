package t_12.backend.api.blockchain.dto;

import java.util.List;

/**
 * Paginated block list response for explorer pages.
 */
public class BlockListResponseDTO {

    private final List<BlockListItemDTO> data;
    private final BlockListPaginationDTO pagination;

    public BlockListResponseDTO(List<BlockListItemDTO> data, BlockListPaginationDTO pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<BlockListItemDTO> getData() {
        return data;
    }

    public BlockListPaginationDTO getPagination() {
        return pagination;
    }
}
