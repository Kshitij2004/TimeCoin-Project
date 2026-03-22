package t_12.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.entity.StakingEvent;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.StakingEventRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Computes wallet balances from confirmed on-chain state. Instead of
 * reading a stored balance column, this derives available/staked/total
 * from the transaction ledger and staking events.
 */
@Service
public class BalanceService {

    private final TransactionRepository transactionRepository;
    private final StakingEventRepository stakingEventRepository;
    private final WalletRepository walletRepository;

    public BalanceService(TransactionRepository transactionRepository,
                          StakingEventRepository stakingEventRepository,
                          WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.stakingEventRepository = stakingEventRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * Derives the full balance breakdown for a wallet address by scanning
     * confirmed transactions and active staking events.
     *
     * available = received - sent - fees - staked
     * total = available + staked
     *
     * @param walletAddress the wallet address to compute balance for
     * @return BalanceResponse with available, staked, and total fields
     * @throws ResourceNotFoundException if the wallet address doesn't exist
     */
    public BalanceResponse getBalance(String walletAddress) {
        if (!walletRepository.existsByWalletAddress(walletAddress)) {
            throw new ResourceNotFoundException("Wallet not found: " + walletAddress);
        }

        BigDecimal received = transactionRepository
                .sumAmountByReceiverAndStatus(walletAddress, Transaction.Status.CONFIRMED);
        BigDecimal sent = transactionRepository
                .sumAmountBySenderAndStatus(walletAddress, Transaction.Status.CONFIRMED);
        BigDecimal fees = transactionRepository
                .sumFeesBySenderAndStatus(walletAddress, Transaction.Status.CONFIRMED);

        BigDecimal staked = computeStakedBalance(walletAddress);

        // null guard — COALESCE should handle it but just in case
        received = received != null ? received : BigDecimal.ZERO;
        sent = sent != null ? sent : BigDecimal.ZERO;
        fees = fees != null ? fees : BigDecimal.ZERO;
        staked = staked != null ? staked : BigDecimal.ZERO;

        BigDecimal available = received.subtract(sent).subtract(fees).subtract(staked);
        BigDecimal total = available.add(staked);

        return new BalanceResponse(walletAddress, available, staked, total);
    }

    /**
     * Net staked = sum of STAKE events minus sum of UNSTAKE events.
     */
    private BigDecimal computeStakedBalance(String walletAddress) {
        BigDecimal totalStaked = stakingEventRepository
                .sumAmountByWalletAddressAndEventType(walletAddress, StakingEvent.EventType.STAKE);
        BigDecimal totalUnstaked = stakingEventRepository
                .sumAmountByWalletAddressAndEventType(walletAddress, StakingEvent.EventType.UNSTAKE);

        totalStaked = totalStaked != null ? totalStaked : BigDecimal.ZERO;
        totalUnstaked = totalUnstaked != null ? totalUnstaked : BigDecimal.ZERO;

        return totalStaked.subtract(totalUnstaked);
    }
}