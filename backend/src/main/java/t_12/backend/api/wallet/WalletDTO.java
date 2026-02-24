package t_12.backend.api.wallet;

import java.math.BigDecimal;

import t_12.backend.entity.Wallet;

public class WalletDTO {

    private Long userId;
    private BigDecimal coinBalance;

    public WalletDTO(Wallet wallet) {
        this.userId = wallet.getUserId();
        this.coinBalance = wallet.getCoinBalance();
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getCoinBalance() {
        return coinBalance;
    }
}