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

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.exception.InsufficientFundsException;
import t_12.backend.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionValidationServiceTest {

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private TransactionValidationService validationService;

    private static final String SENDER = "tc1_sender_abc123";

    //null sender (coinbase)

    @Test
    void validateBalance_nullSender_skipsValidation() {
        assertDoesNotThrow(() ->
                validationService.validateBalance(null, new BigDecimal("1000"), new BigDecimal("1")));

        verify(balanceService, never()).getBalance(null);
    }

    //sufficient funds

    @Test
    void validateBalance_exactBalance_passes() {
        when(balanceService.getBalance(SENDER)).thenReturn(
                balanceResp("100", "0", "100"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, new BigDecimal("90"), new BigDecimal("10")));
    }

    @Test
    void validateBalance_moreThanEnough_passes() {
        when(balanceService.getBalance(SENDER)).thenReturn(
                balanceResp("500", "0", "500"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));
    }

    //insufficient funds

    @Test
    void validateBalance_insufficientFunds_throwsException() {
        when(balanceService.getBalance(SENDER)).thenReturn(
                balanceResp("10", "0", "10"));

        assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));
    }

    @Test
    void validateBalance_insufficientFunds_messageContainsAmounts() {
        when(balanceService.getBalance(SENDER)).thenReturn(
                balanceResp("10", "0", "10"));

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));

        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("51"));
    }

    //staked funds don't count as available

    @Test
    void validateBalance_fundsStaked_usesAvailableNotTotal() {
        // total = 200, but available = 5, staked = 195
        when(balanceService.getBalance(SENDER)).thenReturn(
                balanceResp("5", "195", "200"));

        assertThrows(InsufficientFundsException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("50"), new BigDecimal("1")));
    }

    //wallet not found

    @Test
    void validateBalance_walletNotFound_propagatesException() {
        when(balanceService.getBalance(SENDER))
                .thenThrow(new ResourceNotFoundException("Wallet not found: " + SENDER));

        assertThrows(ResourceNotFoundException.class, () ->
                validationService.validateBalance(SENDER, new BigDecimal("1"), new BigDecimal("0")));
    }

    //edge cases

    @Test
    void validateBalance_zeroAmountAndFee_passes() {
        when(balanceService.getBalance(SENDER)).thenReturn(
                balanceResp("0", "0", "0"));

        assertDoesNotThrow(() ->
                validationService.validateBalance(SENDER, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    //helper

    private BalanceResponse balanceResp(String available, String staked, String total) {
        return new BalanceResponse(SENDER,
                new BigDecimal(available),
                new BigDecimal(staked),
                new BigDecimal(total));
    }
}