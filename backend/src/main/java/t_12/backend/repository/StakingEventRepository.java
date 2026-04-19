package t_12.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import t_12.backend.entity.StakingEvent;

/**
 * Data access methods for staking history.
 */
@Repository
public interface StakingEventRepository extends JpaRepository<StakingEvent, Integer> {
    List<StakingEvent> findByWalletAddress(String walletAddress);
    List<StakingEvent> findByWalletAddressOrderByCreatedAtDescIdDesc(String walletAddress);
    List<StakingEvent> findByEventType(StakingEvent.EventType eventType);

    /** Sum staking amounts by address and event type (STAKE or UNSTAKE) */
    @Query("SELECT COALESCE(SUM(se.amount), 0) FROM StakingEvent se " +
       "WHERE se.walletAddress = :address AND se.eventType = :eventType")
BigDecimal sumAmountByWalletAddressAndEventType(@Param("address") String address,
                                                 @Param("eventType") StakingEvent.EventType eventType);
}
