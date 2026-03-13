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
    // Returns all staking events emitted by a wallet.
    List<StakingEvent> findByWalletAddress(String walletAddress);
    // Returns all events of a given stake or unstake type.
    List<StakingEvent> findByEventType(StakingEvent.EventType eventType);
}
