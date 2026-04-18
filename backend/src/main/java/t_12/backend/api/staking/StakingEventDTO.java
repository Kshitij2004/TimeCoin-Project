package t_12.backend.api.staking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.StakingEvent;

/**
 * API view for a staking event.
 */
public class StakingEventDTO {

    private final Integer id;
    private final String eventType;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;

    public StakingEventDTO(StakingEvent event) {
        this.id = event.getId();
        this.eventType = event.getEventType().name();
        this.amount = event.getAmount();
        this.createdAt = event.getCreatedAt();
    }

    public Integer getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
