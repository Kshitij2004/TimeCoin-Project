package t_12.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.exception.InsufficientFundsException;

@Service
public class TransactionValidationService {

    private final BalanceService balanceService;

    public TransactionValidationService(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    public void validateBalance(String senderAddress, BigDecimal amount, BigDecimal fee) {
        if (senderAddress == null) {
            return;
        }

        BalanceResponse balance = balanceService.getBalance(senderAddress);
        BigDecimal required = amount.add(fee);

        if (balance.getAvailable().compareTo(required) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: available balance is " + balance.getAvailable()
                    + " but " + required + " is required (amount + fee)."
            );
        }
    }
}