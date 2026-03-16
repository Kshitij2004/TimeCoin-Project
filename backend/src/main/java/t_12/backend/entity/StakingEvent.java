package t_12.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Immutable audit log entry for a stake or unstake action. Kept separate
// from the validators table so the current staked_amount can be independently
// verified by summing events - important for ledger integrity.
@Entity
@Table(name = "staking_events")
public class StakingEvent {

    public enum EventType {
        STAKE, UNSTAKE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // references the on-chain wallet identity, not the user account
    @Column(name = "wallet_address", nullable = false, length = 128)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 8)
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getters and setters

    public Integer getId() {
        return id;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public EventType getEventType() {
        return eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}