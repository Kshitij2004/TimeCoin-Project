package t_12.backend.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.exception.InsufficientFundsException;

@ExtendWith(MockitoExtension.class)
public class TransactionValidationServiceTest {

    private BalanceService balanceService;
    private TransactionValidationService validationService;

    @BeforeEach
    void setUp() {
        balanceService = mock(BalanceService.class);
        validationService = new TransactionValidationService(balanceService);
    }

    @Test
    void validateBalance_sufficientFunds_doesNotThrowTest() {
        when(balanceService.getBalance("addr_abc")).thenReturn(
                new BalanceResponse("addr_abc", new BigDecimal("10.00000000"), BigDecimal.ZERO, new BigDecimal("10.00000000")));

        assertDoesNotThrow(()
                -> validationService.validateBalance("addr_abc",
                        new BigDecimal("6.00000000"),
                        new BigDecimal("1.00000000"))
        );
    }

    @Test
    void validateBalance_insufficientFunds_throwsInsufficientFundsExceptionTest() {
        when(balanceService.getBalance("addr_abc")).thenReturn(
                new BalanceResponse("addr_abc", new BigDecimal("5.00000000"), BigDecimal.ZERO, new BigDecimal("5.00000000")));

        assertThrows(InsufficientFundsException.class, ()
                -> validationService.validateBalance("addr_abc",
                        new BigDecimal("4.00000000"),
                        new BigDecimal("2.00000000"))
        );
    }

    @Test
    void validateBalance_exactFunds_doesNotThrowTest() {
        when(balanceService.getBalance("addr_abc")).thenReturn(
                new BalanceResponse("addr_abc", new BigDecimal("5.00000000"), BigDecimal.ZERO, new BigDecimal("5.00000000")));

        assertDoesNotThrow(()
                -> validationService.validateBalance("addr_abc",
                        new BigDecimal("4.00000000"),
                        new BigDecimal("1.00000000"))
        );
    }

    @Test
    void validateBalance_nullSender_skipsCheckEntirelyTest() {
        assertDoesNotThrow(()
                -> validationService.validateBalance(null,
                        new BigDecimal("10.00000000"),
                        new BigDecimal("1.00000000"))
        );
        verify(balanceService, never()).getBalance(null);
    }
}