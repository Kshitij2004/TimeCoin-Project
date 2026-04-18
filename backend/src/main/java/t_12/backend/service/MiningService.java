package t_12.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import t_12.backend.api.mining.MineResponse;
import t_12.backend.api.mining.MiningStatsResponse;
import t_12.backend.entity.MiningAccumulator;
import t_12.backend.exception.CooldownException;
import t_12.backend.repository.MiningAccumulatorRepository;
import t_12.backend.repository.TransactionRepository;

/**
 * Handles click-based mining logic. Each call to mine() increments a per-wallet
 * accumulator row. MiningAggregationScheduler flushes accumulated clicks into a
 * single coinbase transaction per wallet each window.
 */
@Service
public class MiningService {

    private final MiningAccumulatorRepository miningAccumulatorRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Value("${mining.enabled}")
    private boolean miningEnabled;

    @Value("${mining.cooldown-seconds}")
    private int cooldownSeconds;

    @Value("${mining.reward}")
    private BigDecimal baseReward;

    @Value("${mining.halving-enabled}")
    private boolean halvingEnabled;

    @Value("${mining.halving-threshold}")
    private BigDecimal halvingThreshold;

    public MiningService(
            MiningAccumulatorRepository miningAccumulatorRepository,
            TransactionRepository transactionRepository,
            TransactionService transactionService) {
        this.miningAccumulatorRepository = miningAccumulatorRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    /**
     * Records a mining click for the given wallet. Enforces cooldown and
     * increments the accumulator row for the current window.
     *
     * @param walletAddress the wallet address of the miner
     * @return the updated MiningAccumulator row
     * @throws IllegalStateException if mining is disabled
     * @throws CooldownException if the wallet is still within its cooldown
     * period
     */
    public MineResponse mine(String walletAddress) {
        if (!miningEnabled) {
            throw new IllegalStateException("Mining is currently disabled.");
        }

        MiningAccumulator accumulator = miningAccumulatorRepository
                .findById(walletAddress)
                .orElse(null);

        if (accumulator != null) {
            long secondsSinceLast = ChronoUnit.SECONDS.between(
                    accumulator.getLastMinedAt(), LocalDateTime.now());
            if (secondsSinceLast < cooldownSeconds) {
                long retryAfter = cooldownSeconds - secondsSinceLast;
                throw new CooldownException(
                        "Cooldown active. Retry after " + retryAfter + " seconds.", retryAfter);
            }
            accumulator.setClickCount(accumulator.getClickCount() + 1);
            accumulator.setLastMinedAt(LocalDateTime.now());
        } else {
            accumulator = new MiningAccumulator(walletAddress);
        }

        MiningAccumulator saved = miningAccumulatorRepository.save(accumulator);
        return new MineResponse(
                saved.getClickCount(),
                cooldownSeconds, // full cooldown starts now
                "Click recorded. Reward will be issued at the end of the current window."
        );
    }

    /**
     * Flushes all active accumulator rows into coinbase transactions. Called by
     * MiningAggregationScheduler at the end of each window.
     *
     * @param rows all active MiningAccumulator rows
     */
    public void flushAccumulator(List<MiningAccumulator> rows) {
        if (rows.isEmpty()) {
            return;
        }

        BigDecimal totalCoinbaseSupply = transactionRepository.sumConfirmedCoinbaseSupply();

        for (MiningAccumulator row : rows) {
            BigDecimal reward = calculateReward(row.getClickCount(), totalCoinbaseSupply);
            createCoinbaseTx(row.getWalletAddress(), reward);
            miningAccumulatorRepository.delete(row);
        }
    }

    /**
     * Returns mining stats for a wallet: total coins mined, total mine count,
     * and seconds remaining in the current cooldown.
     *
     * @param walletAddress the wallet address to look up
     * @return MiningStatsResponse DTO
     */
    public MiningStatsResponse getStats(String walletAddress) {
        BigDecimal totalMined = transactionRepository
                .sumConfirmedCoinbaseByReceiver(walletAddress);
        long totalCount = transactionRepository
                .countConfirmedCoinbaseByReceiver(walletAddress);

        long cooldownRemaining = 0;
        MiningAccumulator accumulator = miningAccumulatorRepository
                .findById(walletAddress)
                .orElse(null);
        if (accumulator != null) {
            long secondsSinceLast = ChronoUnit.SECONDS.between(
                    accumulator.getLastMinedAt(), LocalDateTime.now());
            cooldownRemaining = Math.max(0, cooldownSeconds - secondsSinceLast);
        }

        return new MiningStatsResponse(totalMined, totalCount, cooldownRemaining);
    }

    /**
     * Calculates the reward for a flush. Applies halving if enabled: reward
     * scales down as total coinbase supply grows relative to halvingThreshold.
     * Formula: baseReward * clickCount * (1 / (1 + supply / threshold))
     *
     * @param clickCount the number of clicks in this window
     * @param totalCoinbaseSupply total confirmed coinbase supply across all
     * wallets
     * @return the calculated reward amount
     */
    private BigDecimal calculateReward(int clickCount, BigDecimal totalCoinbaseSupply) {
        BigDecimal reward = baseReward.multiply(BigDecimal.valueOf(clickCount));

        if (halvingEnabled && totalCoinbaseSupply.compareTo(BigDecimal.ZERO) > 0) {
            // scale factor = 1 / (1 + supply / threshold)
            BigDecimal ratio = totalCoinbaseSupply.divide(halvingThreshold, 8, RoundingMode.HALF_UP);
            BigDecimal scaleFactor = BigDecimal.ONE.divide(
                    BigDecimal.ONE.add(ratio), 8, RoundingMode.HALF_UP);
            reward = reward.multiply(scaleFactor).setScale(8, RoundingMode.HALF_UP);
        }

        return reward;
    }

    /**
     * Creates and enqueues a coinbase transaction (null sender) for the given
     * wallet and reward amount.
     *
     * @param walletAddress the miner's wallet address
     * @param reward the reward amount to credit
     */
    private void createCoinbaseTx(String walletAddress, BigDecimal reward) {
        transactionService.createCoinbaseTransaction(walletAddress, reward);
    }
}
