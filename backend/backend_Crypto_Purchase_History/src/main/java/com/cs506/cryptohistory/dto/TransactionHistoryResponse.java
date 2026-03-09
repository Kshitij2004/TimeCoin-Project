package com.cs506.cryptohistory.dto;

import java.util.List;

public record TransactionHistoryResponse(
    List<TransactionHistoryItem> data,
    PaginationDto pagination
) {
}
