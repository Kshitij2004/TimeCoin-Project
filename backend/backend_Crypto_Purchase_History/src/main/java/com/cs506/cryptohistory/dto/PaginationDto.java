package com.cs506.cryptohistory.dto;

public record PaginationDto(
    int page,
    int limit,
    int total,
    int totalPages
) {
}
