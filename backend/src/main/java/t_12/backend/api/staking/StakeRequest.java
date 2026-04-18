package t_12.backend.api.staking;

import java.math.BigDecimal;

/**
 * Request payload for POST /api/staking/stake.
 */
public class StakeRequest {

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
