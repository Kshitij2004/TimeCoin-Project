package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import t_12.backend.api.transaction.dto.TransactionHistoryResponseDTO;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.TransactionRepository;

/**
 * Unit tests for TransactionHistoryService.
 */
@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionHistoryService transactionHistoryService;

    @Test
    void getUserTransactionsReturnsMostRecentWithPagination() {
        Transaction buy = new Transaction();
        buy.setTransactionType(Transaction.TransactionType.BUY);
        buy.setAmount(new BigDecimal("0.25000000"));
        buy.setPriceAtTime(new BigDecimal("66000.00"));
        buy.setTimestamp(LocalDateTime.parse("2026-02-23T12:00:00"));

        Transaction sell = new Transaction();
        sell.setTransactionType(Transaction.TransactionType.SELL);
        sell.setAmount(new BigDecimal("0.50000000"));
        sell.setPriceAtTime(new BigDecimal("65500.00"));
        sell.setTimestamp(LocalDateTime.parse("2026-02-23T11:00:00"));

        when(transactionRepository.findByUserIdAndTransactionTypeInOrderByTimestampDescIdDesc(
                1,
                List.of(Transaction.TransactionType.BUY, Transaction.TransactionType.SELL),
                PageRequest.of(0, 2)
        )).thenReturn(new PageImpl<>(List.of(buy, sell), PageRequest.of(0, 2), 3));

        TransactionHistoryResponseDTO response = transactionHistoryService.getUserTransactions(1, 1, 2);

        assertEquals(2, response.getData().size());
        assertEquals("BUY", response.getData().get(0).getType());
        assertEquals(3, response.getPagination().getTotal());
        assertEquals(2, response.getPagination().getTotalPages());
    }

    @Test
    void getUserTransactionsRequiresAuthenticatedUser() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionHistoryService.getUserTransactions(null, 1, 20)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Authenticated user is required", exception.getMessage());
    }

    @Test
    void getUserTransactionsValidatesPagination() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionHistoryService.getUserTransactions(1, 0, 20)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("page must be a positive integer", exception.getMessage());
    }
}
