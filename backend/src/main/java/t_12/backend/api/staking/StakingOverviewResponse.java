package t_12.backend.api.staking;

import java.math.BigDecimal;
import java.util.List;

/**
 * Current staking state plus event history for a wallet.
 */
public class StakingOverviewResponse {

    private final String walletAddress;
    private final BigDecimal available;
    private final BigDecimal staked;
    private final BigDecimal total;
    private final List<StakingEventDTO> events;

    public StakingOverviewResponse(
            String walletAddress,
            BigDecimal available,
            BigDecimal staked,
            BigDecimal total,
            List<StakingEventDTO> events) {
        this.walletAddress = walletAddress;
        this.available = available;
        this.staked = staked;
        this.total = total;
        this.events = events;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getStaked() {
        return staked;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<StakingEventDTO> getEvents() {
        return events;
    }
}
