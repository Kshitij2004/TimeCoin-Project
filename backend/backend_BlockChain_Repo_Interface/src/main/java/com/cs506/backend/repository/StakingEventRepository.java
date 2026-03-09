package com.cs506.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cs506.backend.entity.StakingEvent;

@Repository
public interface StakingEventRepository extends JpaRepository<StakingEvent, Integer> {
    List<StakingEvent> findByWalletAddress(String walletAddress);
    List<StakingEvent> findByEventType(StakingEvent.EventType eventType);
}
