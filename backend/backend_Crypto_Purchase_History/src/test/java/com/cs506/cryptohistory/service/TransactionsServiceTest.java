package com.cs506.cryptohistory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.cs506.cryptohistory.dto.TransactionHistoryResponse;
import com.cs506.cryptohistory.exception.TransactionHistoryException;
import com.cs506.cryptohistory.model.TransactionRow;
import com.cs506.cryptohistory.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionsServiceTest {

    @Mock
    private TransactionHistoryRepository repository;

    private TransactionsService transactionsService;

    @BeforeEach
    void setUp() {
        transactionsService = new TransactionsService(repository);
    }

    @Test
    void getUserTransactionsReturnsMostRecentWithPagination() {
        when(repository.countByUserId(1)).thenReturn(3);
        when(repository.findByUserId(1, 2, 0)).thenReturn(List.of(
            new TransactionRow("BUY", new BigDecimal("0.25000000"), new BigDecimal("66000.00"), LocalDateTime.parse("2026-02-23T12:00:00")),
            new TransactionRow("SELL", new BigDecimal("0.50000000"), new BigDecimal("65500.00"), LocalDateTime.parse("2026-02-23T11:00:00"))
        ));

        TransactionHistoryResponse response = transactionsService.getUserTransactions(1, 1, 2);

        assertEquals(2, response.data().size());
        assertEquals("BUY", response.data().get(0).type());
        assertEquals(3, response.pagination().total());
        assertEquals(2, response.pagination().totalPages());
    }

    @Test
    void getUserTransactionsRequiresAuthenticatedUser() {
        TransactionHistoryException exception = assertThrows(
            TransactionHistoryException.class,
            () -> transactionsService.getUserTransactions(null, 1, 20)
        );
        assertEquals(401, exception.getStatusCode());
        assertEquals("Authenticated user is required", exception.getMessage());
    }

    @Test
    void getUserTransactionsValidatesPagination() {
        TransactionHistoryException exception = assertThrows(
            TransactionHistoryException.class,
            () -> transactionsService.getUserTransactions(1, 0, 20)
        );
        assertEquals(400, exception.getStatusCode());
        assertEquals("page must be a positive integer", exception.getMessage());
    }
}
