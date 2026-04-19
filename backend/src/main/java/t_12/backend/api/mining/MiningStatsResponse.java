package t_12.backend.api.mining;

import java.math.BigDecimal;

public class MiningStatsResponse {

    private final BigDecimal totalMined;
    private final long totalCount;
    private final long cooldownRemaining;

    public MiningStatsResponse(BigDecimal totalMined, long totalCount, long cooldownRemaining) {
        this.totalMined = totalMined;
        this.totalCount = totalCount;
        this.cooldownRemaining = cooldownRemaining;
    }

    public BigDecimal getTotalMined() {
        return totalMined;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getCooldownRemaining() {
        return cooldownRemaining;
    }
}
