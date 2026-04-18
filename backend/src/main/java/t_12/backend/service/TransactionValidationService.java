package t_12.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.InvalidNonceException;
import t_12.backend.exception.InsufficientFundsException;
import t_12.backend.repository.TransactionRepository;

/**
 * Validates transactions before creation. Uses spendable balance
 * (available minus pending outgoing) to prevent double-spending.
 */
@Service
public class TransactionValidationService {

    private final BalanceService balanceService;
    private final TransactionRepository transactionRepository;

    public TransactionValidationService(
            BalanceService balanceService,
            TransactionRepository transactionRepository) {
        this.balanceService = balanceService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Validates that the sender's spendable balance can cover the transaction.
     * Spendable balance accounts for pending outgoing transactions, preventing
     * double-spend by submitting multiple transactions before the first confirms.
     *
     * @param senderAddress the wallet address of the sender (nullable for coinbase)
     * @param amount the amount to transfer
     * @param fee the transaction fee
     * @throws InsufficientFundsException if the sender's spendable balance is too low
     */
    public void validateBalance(String senderAddress, BigDecimal amount, BigDecimal fee) {
        if (senderAddress == null) {
            return;
        }

        BigDecimal spendable = balanceService.getSpendableBalance(senderAddress);
        BigDecimal required = amount.add(fee);

        if (spendable.compareTo(required) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: spendable balance is " + spendable
                    + " but " + required + " is required (amount + fee).");
        }
    }

    /**
     * Validates the sender's nonce against the expected sequence number.
     *
     * @param senderAddress the sender wallet address
     * @param nonce the nonce provided in the transaction
     */
    public void validateNonce(String senderAddress, Integer nonce) {
        if (senderAddress == null) {
            return;
        }

        long confirmedSentCount = transactionRepository.countBySenderAddressAndStatus(
                senderAddress,
                Transaction.Status.CONFIRMED
        );
        long expectedNonce = confirmedSentCount + 1;

        if (nonce == null || nonce < 0 || nonce.longValue() != expectedNonce) {
            throw new InvalidNonceException(expectedNonce, nonce);
        }
    }
}
