package t_12.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import t_12.backend.api.transaction.PaginationDTO;
import t_12.backend.api.transaction.TransactionHistoryItem;
import t_12.backend.api.transaction.TransactionHistoryResponse;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.TransactionRepository;

/**
 * Returns paginated buy and sell history for a user.
 */
@Service
public class TransactionHistoryService {

    private static final List<Transaction.TransactionType> PURCHASE_TYPES = List.of(
            Transaction.TransactionType.BUY,
            Transaction.TransactionType.SELL
    );

    private final TransactionRepository transactionRepository;

    public TransactionHistoryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionHistoryResponse getUserTransactions(
            Integer userId,
            Integer page,
            Integer limit) {
        if (userId == null || userId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        int resolvedPage = page == null ? 1 : page;
        int resolvedLimit = limit == null ? 20 : limit;

        if (resolvedPage < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page must be a positive integer");
        }
        if (resolvedLimit < 1 || resolvedLimit > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "limit must be an integer between 1 and 100");
        }

        Page<Transaction> results = transactionRepository.findByUserIdAndTransactionTypeInOrderByTimestampDescIdDesc(
                userId,
                PURCHASE_TYPES,
                PageRequest.of(resolvedPage - 1, resolvedLimit)
        );

        List<TransactionHistoryItem> items = results.getContent()
                .stream()
                .map(TransactionHistoryItem::new)
                .toList();

        return new TransactionHistoryResponse(
                items,
                new PaginationDTO(
                        resolvedPage,
                        resolvedLimit,
                        results.getTotalElements(),
                        results.getTotalPages()
                )
        );
    }
}
