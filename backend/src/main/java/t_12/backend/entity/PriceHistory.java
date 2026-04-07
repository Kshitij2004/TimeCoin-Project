package t_12.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Records a snapshot of the coin price at a point in time.
 * One row is created per price recalculation for charting.
 */
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public PriceHistory() {}

    public PriceHistory(BigDecimal price, LocalDateTime recordedAt) {
        this.price = price;
        this.recordedAt = recordedAt;
    }

    public Integer getId() { return id; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getRecordedAt() { return recordedAt; }

    public void setId(Integer id) { this.id = id; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}