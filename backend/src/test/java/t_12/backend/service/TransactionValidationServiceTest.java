package t_12.backend.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.ApiException;
import t_12.backend.exception.InsufficientFundsException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionValidationServiceTest {

    @Mock
    private BalanceService balanceService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionValidationService validationService;

    private static final String SENDER = "tc1_sender_abc123";

    // null sender (coinbase)

    @Test
    void validateBalance_nullSender_skipsValidation() {
        assertDoesNotThrow(() ->
                validationService.validateBalance(null, new BigDecimal("1000"), new BigDecimal("1")));

        verify(balanceService, never()).getSpendableBalance(null);
    }

    // sufficient funds

    @Test
    void validateBalance_exactBalance_passes() {
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("100"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, new BigDecimal("90"), new BigDecimal("10")));
    }

    @Test
    void validateBalance_moreThanEnough_passes() {
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("500"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));
    }

    // insufficient funds

    @Test
    void validateBalance_insufficientFunds_throwsException() {
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("10"));

        assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));
    }

    @Test
    void validateBalance_insufficientFunds_messageContainsAmounts() {
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("10"));

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));

        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("51"));
    }

    // pending outgoing reduces spendable

    @Test
    void validateBalance_pendingOutgoingReducesSpendable() {
        // 100 available, but 10 pending outgoing → spendable = 90
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("90"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, new BigDecimal("90"), BigDecimal.ZERO));
    }

    @Test
    void validateBalance_pendingOutgoingCausesInsufficientFunds() {
        // 100 available, 10 pending outgoing → spendable = 90, trying to send 95
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("90"));

        assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("95"), BigDecimal.ZERO));
    }

    @Test
    void validateBalance_multiplePendingTransactionsStack() {
        // 100 available, 60 total pending outgoing → spendable = 40
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("40"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, new BigDecimal("40"), BigDecimal.ZERO));

        assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("41"), BigDecimal.ZERO));
    }

    // staked funds don't count as spendable

    @Test
    void validateBalance_fundsStaked_usesSpendableNotTotal() {
        // total = 200, available = 5, staked = 195, no pending → spendable = 5
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(new BigDecimal("5"));

        assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));
    }

    // wallet not found

    @Test
    void validateBalance_walletNotFound_propagatesException() {
        when(balanceService.getSpendableBalance(SENDER))
                .thenThrow(new ResourceNotFoundException("Wallet not found: " + SENDER));

        assertThrows(ResourceNotFoundException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("1"), new BigDecimal("0")));
    }

    // edge cases

    @Test
    void validateBalance_zeroAmountAndFee_passes() {
        when(balanceService.getSpendableBalance(SENDER)).thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void validateNonce_validNonce_passes() {
        when(transactionRepository.countBySenderAddressAndStatus(SENDER, Transaction.Status.CONFIRMED))
                .thenReturn(3L);

        assertDoesNotThrow(() -> validationService.validateNonce(SENDER, 4));
    }

    @Test
    void validateNonce_duplicateNonce_rejectedWithExpectedAndProvided() {
        when(transactionRepository.countBySenderAddressAndStatus(SENDER, Transaction.Status.CONFIRMED))
                .thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class, () -> validationService.validateNonce(SENDER, 3));

        assertTrue(ex.getMessage().contains("expected 4"));
        assertTrue(ex.getMessage().contains("received 3"));
    }

    @Test
    void validateNonce_skippedNonce_rejectedWithExpectedAndProvided() {
        when(transactionRepository.countBySenderAddressAndStatus(SENDER, Transaction.Status.CONFIRMED))
                .thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class, () -> validationService.validateNonce(SENDER, 5));

        assertTrue(ex.getMessage().contains("expected 4"));
        assertTrue(ex.getMessage().contains("received 5"));
    }
}
