package t_12.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.InsufficientFundsException;

/**
 * Service reponsible for validating transactions before they are created.
 * Checks that the sender has sufficient funds to cover the amount and fee.
 */
@Service
public class TransactionValidationService {

    private final WalletService walletService;

    public TransactionValidationService(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Validates that the sender's wallet balance can cover the transcation.
     * Skips the check if senderAddress is null.
     *
     * @param senderAddress the wallet address of the transaction sender
     * (nullable)
     * @param amount the amount to be sent
     * @param fee the transaction fee throws InsufficientFundsException if the
     * sender's balance is too low
     */
    public void validateBalance(String senderAddress, BigDecimal amount, BigDecimal fee) {
        if (senderAddress == null) {
            // No sender means no balance check needed
        }

        Wallet wallet = walletService.getWalletBySenderAddress(senderAddress);
        BigDecimal required = amount.add(fee);

        if (wallet.getCoinBalance().compareTo(required) < 0) {
            throw new InsufficientFundsException(
                    "Sender has insufficient funds: required " + required
                    + ", but balance is " + wallet.getCoinBalance());
        }
    }
}
