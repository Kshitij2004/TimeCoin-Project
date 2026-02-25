package com.cs506.cryptohistory.service;

import com.cs506.cryptohistory.dto.PaginationDto;
import com.cs506.cryptohistory.dto.TransactionHistoryItem;
import com.cs506.cryptohistory.dto.TransactionHistoryResponse;
import com.cs506.cryptohistory.exception.TransactionHistoryException;
import com.cs506.cryptohistory.model.TransactionRow;
import com.cs506.cryptohistory.repository.TransactionHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TransactionsService {

    private final TransactionHistoryRepository repository;

    public TransactionsService(TransactionHistoryRepository repository) {
        this.repository = repository;
    }

    public TransactionHistoryResponse getUserTransactions(Integer userId, Integer page, Integer limit) {
        if (userId == null || userId <= 0) {
            throw new TransactionHistoryException("Authenticated user is required", 401);
        }

        int resolvedPage = page == null ? 1 : page;
        int resolvedLimit = limit == null ? 20 : limit;

        if (resolvedPage < 1) {
            throw new TransactionHistoryException("page must be a positive integer", 400);
        }
        if (resolvedLimit < 1 || resolvedLimit > 100) {
            throw new TransactionHistoryException("limit must be an integer between 1 and 100", 400);
        }

        int offset = (resolvedPage - 1) * resolvedLimit;
        int total = repository.countByUserId(userId);
        List<TransactionRow> rows = repository.findByUserId(userId, resolvedLimit, offset);
        List<TransactionHistoryItem> items = rows.stream()
            .map(row -> new TransactionHistoryItem(
                row.transactionType(),
                row.quantity(),
                row.priceUsd(),
                row.createdAt()
            ))
            .toList();

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / resolvedLimit);
        return new TransactionHistoryResponse(
            items,
            new PaginationDto(resolvedPage, resolvedLimit, total, totalPages)
        );
    }
}
