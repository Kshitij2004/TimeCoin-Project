package t_12.backend.api.coin;

import java.math.BigDecimal;

import t_12.backend.entity.Wallet;

/**
 * Wallet details returned after a purchase.
 */
public class PurchaseWalletDTO {

    private final Integer userId;
    private final String walletAddress;
    private final BigDecimal coinBalance;

    public PurchaseWalletDTO(Wallet wallet, BigDecimal ledgerBalance) {
        this.userId = wallet.getUserId();
        this.walletAddress = wallet.getWalletAddress();
        this.coinBalance = ledgerBalance;
    }

    public Integer getUserId() { return userId; }
    public String getWalletAddress() { return walletAddress; }
    public BigDecimal getCoinBalance() { return coinBalance; }
}