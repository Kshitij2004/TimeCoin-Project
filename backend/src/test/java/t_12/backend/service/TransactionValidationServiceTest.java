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

import t_12.backend.entity.Wallet;
import t_12.backend.exception.InsufficientFundsException;

@ExtendWith(MockitoExtension.class)
public class TransactionValidationServiceTest {

    private WalletService walletService;
    private TransactionValidationService validationService;

    @BeforeEach
    void setUp() {
        walletService = mock(WalletService.class);
        validationService = new TransactionValidationService(walletService);
    }

    @Test
    void validateBalance_sufficientFunds_doesNotThrowTest() {
        Wallet wallet = new Wallet();
        wallet.setCoinBalance(new BigDecimal("10.00000000"));
        when(walletService.getWalletBySenderAddress("addr_abc")).thenReturn(wallet);

        assertDoesNotThrow(()
                -> validationService.validateBalance("addr_abc",
                        new BigDecimal("6.00000000"),
                        new BigDecimal("1.00000000"))
        );
    }

    @Test
    void validateBalance_insufficientFunds_throwsInsufficientFundsExceptionTest() {
        Wallet wallet = new Wallet();
        wallet.setCoinBalance(new BigDecimal("5.00000000"));
        when(walletService.getWalletBySenderAddress("addr_abc")).thenReturn(wallet);

        assertThrows(InsufficientFundsException.class, ()
                -> validationService.validateBalance("addr_abc",
                        new BigDecimal("4.00000000"),
                        new BigDecimal("2.00000000"))
        );
    }

    @Test
    void validateBalance_exactFunds_doesNotThrowTest() {
        Wallet wallet = new Wallet();
        wallet.setCoinBalance(new BigDecimal("5.00000000"));
        when(walletService.getWalletBySenderAddress("addr_abc")).thenReturn(wallet);

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
        verify(walletService, never()).getWalletBySenderAddress(null);
    }
}
