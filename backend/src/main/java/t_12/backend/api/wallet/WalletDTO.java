package t_12.backend.api.wallet;

import java.math.BigDecimal;

import t_12.backend.entity.Wallet;

/**
 * Data Transfer Object for Wallet information.
 */
public class WalletDTO {

    private final Integer userId;
    private final String walletAddress;
    private final String publicKey;
    private final BigDecimal coinBalance;

    /**
     * @param wallet the Wallet entity to convert
     * @param coinBalance ledger-derived spendable balance from BalanceService
     */
    public WalletDTO(Wallet wallet, BigDecimal coinBalance) {
        this.userId = wallet.getUserId();
        this.walletAddress = wallet.getWalletAddress();
        this.publicKey = wallet.getPublicKey();
        this.coinBalance = coinBalance;
    }

    public Integer getUserId() {
        return userId;
    }

    public BigDecimal getCoinBalance() {
        return coinBalance;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getPublicKey() {
        return publicKey;
    }
}
