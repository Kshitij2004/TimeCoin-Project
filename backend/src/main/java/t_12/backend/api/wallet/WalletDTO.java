package t_12.backend.api.wallet;

import java.math.BigDecimal;

import t_12.backend.entity.Wallet;

/**
 * Data Transfer Object for Wallet information.
 */
public class WalletDTO {

    private final Integer userId;
    private final BigDecimal coinBalance;

    /**
     * Constructs a WalletDTO from a Wallet entity.
     *
     * @param wallet the Wallet entity to convert
     */
    public WalletDTO(Wallet wallet) {
        this.userId = wallet.getUserId();
        this.coinBalance = wallet.getCoinBalance();
    }

    public Integer getUserId() {
        return userId;
    }

    public BigDecimal getCoinBalance() {
        return coinBalance;
    }
}
