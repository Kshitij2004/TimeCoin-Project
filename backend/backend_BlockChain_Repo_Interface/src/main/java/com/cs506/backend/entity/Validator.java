package com.cs506.backend.entity;

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

@Entity
@Table(name = "validators")
public class Validator {

    public enum Status {
        ACTIVE, INACTIVE, JAILED, UNSTAKING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "wallet_address", nullable = false, unique = true, length = 128)
    private String walletAddress;

    @Column(name = "staked_amount", nullable = false, precision = 18, scale = 8)
    private BigDecimal stakedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_selected_at")
    private LocalDateTime lastSelectedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public BigDecimal getStakedAmount() {
        return stakedAmount;
    }

    public void setStakedAmount(BigDecimal stakedAmount) {
        this.stakedAmount = stakedAmount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLastSelectedAt() {
        return lastSelectedAt;
    }

    public void setLastSelectedAt(LocalDateTime lastSelectedAt) {
        this.lastSelectedAt = lastSelectedAt;
    }
}
