package t_12.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.InsufficientFundsException;

/**
 * Service responsible for validating transactions before they are created.
 * Checks that the sender has sufficient funds to cover the amount and fee.
 */
@Service
public class TransactionValidationService {

    private final WalletService walletService;

    public TransactionValidationService(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Validates that the sender's wallet balance can cover the transaction.
     * Skips the check if senderAddress is null (coinbase/minting transactions
     * have no sender).
     *
     * @param senderAddress the wallet address of the sender (nullable)
     * @param amount the amount to transfer
     * @param fee the transaction fee
     * @throws InsufficientFundsException if the sender's balance is too low
     */
    public void validateBalance(String senderAddress, BigDecimal amount, BigDecimal fee) {
        if (senderAddress == null) {
            // coinbase transactions mint new coins — no sender to check
            return;
        }

        // !! TODO: swap to BalanceService.getBalance(senderAddress).getAvailable()
        // !! once ledger-derived balance issue lands.
        Wallet wallet = walletService.getWalletBySenderAddress(senderAddress);
        BigDecimal required = amount.add(fee);

        if (wallet.getCoinBalance().compareTo(required) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: balance is " + wallet.getCoinBalance()
                    + " but " + required + " is required (amount + fee)."
            );
        }
    }
}
