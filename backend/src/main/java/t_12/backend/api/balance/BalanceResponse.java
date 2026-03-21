package t_12.backend.api.balance;

import java.math.BigDecimal;

/**
 * Response object for wallet balance queries. All values are
 * derived from confirmed on-chain state, not stored balances.
 */
public class BalanceResponse {

    private String walletAddress;
    private BigDecimal available;
    private BigDecimal staked;
    private BigDecimal total;

    public BalanceResponse() {}

    public BalanceResponse(String walletAddress, BigDecimal available, BigDecimal staked, BigDecimal total) {
        this.walletAddress = walletAddress;
        this.available = available;
        this.staked = staked;
        this.total = total;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public void setAvailable(BigDecimal available) {
        this.available = available;
    }

    public BigDecimal getStaked() {
        return staked;
    }

    public void setStaked(BigDecimal staked) {
        this.staked = staked;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}