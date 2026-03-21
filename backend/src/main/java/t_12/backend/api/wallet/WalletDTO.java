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
    // !! TODO: swap coinBalance to BalanceService.getBalance(walletAddress).getAvailable()
    // !! once ledger-derived balance issue lands.
    private final BigDecimal coinBalance;

    /**
     * Constructs a WalletDTO from a Wallet entity.
     *
     * @param wallet the Wallet entity to convert
     */
    public WalletDTO(Wallet wallet) {
        this.userId = wallet.getUserId();
        this.walletAddress = wallet.getWalletAddress();
        this.publicKey = wallet.getPublicKey();
        this.coinBalance = wallet.getCoinBalance();
    }

    public Integer getUserId() {
        return userId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getPublicKey() {
        return publicKey;
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
