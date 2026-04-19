package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.api.staking.StakingEventDTO;
import t_12.backend.api.staking.StakingOverviewResponse;
import t_12.backend.entity.StakingEvent;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.StakingEventRepository;

/**
 * Handles staking and unstaking flows backed by immutable staking events.
 */
@Service
public class StakingService {

    private final StakingEventRepository stakingEventRepository;
    private final BalanceService balanceService;
    private final WalletService walletService;

    public StakingService(
            StakingEventRepository stakingEventRepository,
            BalanceService balanceService,
            WalletService walletService) {
        this.stakingEventRepository = stakingEventRepository;
        this.balanceService = balanceService;
        this.walletService = walletService;
    }

    /**
     * Locks funds from available balance by recording a STAKE event.
     */
    @Transactional
    public StakingOverviewResponse stake(String walletAddress, BigDecimal amount) {
        validatePositiveAmount(amount);
        walletService.getWalletByAddress(walletAddress);

        BalanceResponse before = balanceService.getBalance(walletAddress);
        if (before.getAvailable().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient available balance to stake");
        }

        createEvent(walletAddress, StakingEvent.EventType.STAKE, amount);
        return getStakingOverview(walletAddress);
    }

    /**
     * Releases locked funds back to available balance by recording an UNSTAKE event.
     */
    @Transactional
    public StakingOverviewResponse unstake(String walletAddress, BigDecimal amount) {
        validatePositiveAmount(amount);
        walletService.getWalletByAddress(walletAddress);

        BalanceResponse before = balanceService.getBalance(walletAddress);
        if (before.getStaked().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient staked balance to unstake");
        }

        createEvent(walletAddress, StakingEvent.EventType.UNSTAKE, amount);
        return getStakingOverview(walletAddress);
    }

    /**
     * Returns current staking balance and event history for a wallet.
     */
    @Transactional(readOnly = true)
    public StakingOverviewResponse getStakingOverview(String walletAddress) {
        walletService.getWalletByAddress(walletAddress);
        BalanceResponse balance = balanceService.getBalance(walletAddress);
        List<StakingEventDTO> events = stakingEventRepository
                .findByWalletAddressOrderByCreatedAtDescIdDesc(walletAddress)
                .stream()
                .map(StakingEventDTO::new)
                .toList();

        return new StakingOverviewResponse(
                walletAddress,
                balance.getAvailable(),
                balance.getStaked(),
                balance.getTotal(),
                events
        );
    }

    private void createEvent(String walletAddress, StakingEvent.EventType eventType, BigDecimal amount) {
        StakingEvent event = new StakingEvent();
        event.setWalletAddress(walletAddress);
        event.setEventType(eventType);
        event.setAmount(amount);
        event.setCreatedAt(LocalDateTime.now());
        stakingEventRepository.save(event);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount must be greater than zero");
        }
    }
}
