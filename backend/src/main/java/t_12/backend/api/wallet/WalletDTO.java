package t_12.backend.api.wallet;

import java.math.BigDecimal;

import t_12.backend.entity.Wallet;

public class WalletDTO {

    private Integer userId;
    private BigDecimal coinBalance;

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