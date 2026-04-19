package t_12.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mining_accumulator")
public class MiningAccumulator {

    @Id
    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "click_count", nullable = false)
    private int clickCount;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "last_mined_at", nullable = false)
    private LocalDateTime lastMinedAt;

    public MiningAccumulator() {
    }

    public MiningAccumulator(String walletAddress) {
        this.walletAddress = walletAddress;
        this.clickCount = 1;
        this.windowStart = LocalDateTime.now();
        this.lastMinedAt = LocalDateTime.now();
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public int getClickCount() {
        return clickCount;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public LocalDateTime getLastMinedAt() {
        return lastMinedAt;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }

    public void setLastMinedAt(LocalDateTime lastMinedAt) {
        this.lastMinedAt = lastMinedAt;
    }
}
