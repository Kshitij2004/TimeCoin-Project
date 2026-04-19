package t_12.backend.api.coin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.PriceHistory;

/**
 * Frontend-friendly representation of a price data point.
 * Used by GET /api/coins/price-history.
 */
public class PriceHistoryDTO {

    private BigDecimal price;
    private LocalDateTime recordedAt;

    public PriceHistoryDTO() {}

    public PriceHistoryDTO(PriceHistory entity) {
        this.price = entity.getPrice();
        this.recordedAt = entity.getRecordedAt();
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}