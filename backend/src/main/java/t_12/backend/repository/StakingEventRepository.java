package t_12.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.StakingEvent;

/**
 * Data access methods for staking history.
 */
@Repository
public interface StakingEventRepository extends JpaRepository<StakingEvent, Integer> {
    List<StakingEvent> findByWalletAddress(String walletAddress);
    List<StakingEvent> findByEventType(StakingEvent.EventType eventType);
}
