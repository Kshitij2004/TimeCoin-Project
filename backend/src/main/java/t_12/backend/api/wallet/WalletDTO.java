package t_12.backend.api.wallet;

import t_12.backend.entity.Wallet;
import java.math.BigDecimal;

public class WalletDTO {

    private Long userId;
    private BigDecimal coinBalance;

    public WalletDTO(Wallet wallet) {
        this.userId = wallet.getUserId();
        this.coinBalance = wallet.getCoinBalance();
    }

    public Long getUserId() { return userId; }
    public BigDecimal getCoinBalance() { return coinBalance; }
}