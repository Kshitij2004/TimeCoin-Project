package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import t_12.backend.api.mining.MineResponse;
import t_12.backend.api.mining.MiningStatsResponse;
import t_12.backend.entity.MiningAccumulator;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.CooldownException;
import t_12.backend.repository.MiningAccumulatorRepository;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
public class MiningServiceTest {

    @Mock
    private MiningAccumulatorRepository miningAccumulatorRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private MiningService miningService;

    @BeforeEach
    void setUp() {
        // @Value fields aren't populated in unit tests; inject them manually
        ReflectionTestUtils.setField(miningService, "miningEnabled", true);
        ReflectionTestUtils.setField(miningService, "cooldownSeconds", 5);
        ReflectionTestUtils.setField(miningService, "baseReward", new BigDecimal("1.00000000"));
        ReflectionTestUtils.setField(miningService, "halvingEnabled", false);
        ReflectionTestUtils.setField(miningService, "halvingThreshold", new BigDecimal("100000.00000000"));
    }

    @Test
    void mine_firstClick_createsNewAccumulatorRow() {
        String wallet = "addr_1";
        when(miningAccumulatorRepository.findById(wallet)).thenReturn(Optional.empty());
        when(miningAccumulatorRepository.save(any(MiningAccumulator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MineResponse response = miningService.mine(wallet);

        assertEquals(1, response.getClickCount());
        assertEquals(5, response.getCooldownRemaining());
        ArgumentCaptor<MiningAccumulator> captor = ArgumentCaptor.forClass(MiningAccumulator.class);
        verify(miningAccumulatorRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getClickCount());
    }

    @Test
    void mine_afterCooldown_incrementsClickCount() {
        String wallet = "addr_1";
        MiningAccumulator existing = new MiningAccumulator(wallet);
        existing.setClickCount(3);
        // Last mined 10 seconds ago - well past the 5 second cooldown
        existing.setLastMinedAt(LocalDateTime.now().minusSeconds(10));

        when(miningAccumulatorRepository.findById(wallet)).thenReturn(Optional.of(existing));
        when(miningAccumulatorRepository.save(any(MiningAccumulator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MineResponse response = miningService.mine(wallet);

        assertEquals(4, response.getClickCount());
        ArgumentCaptor<MiningAccumulator> captor = ArgumentCaptor.forClass(MiningAccumulator.class);
        verify(miningAccumulatorRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getClickCount());
    }

    @Test
    void mine_withinCooldown_throwsCooldownException() {
        String wallet = "addr_1";
        MiningAccumulator existing = new MiningAccumulator(wallet);
        // Last mined 2 seconds ago - still within the 5 second cooldown
        existing.setLastMinedAt(LocalDateTime.now().minusSeconds(2));

        when(miningAccumulatorRepository.findById(wallet)).thenReturn(Optional.of(existing));

        CooldownException ex = assertThrows(CooldownException.class,
                () -> miningService.mine(wallet));
        assertTrue(ex.getRetryAfter() > 0);
        verify(miningAccumulatorRepository, never()).save(any());
    }

    @Test
    void mine_atExactCooldownBoundary_succeeds() {
        String wallet = "addr_1";
        MiningAccumulator existing = new MiningAccumulator(wallet);
        // Last mined exactly 5 seconds ago — code uses <, so 5s-ago should pass
        existing.setLastMinedAt(LocalDateTime.now().minusSeconds(5));

        when(miningAccumulatorRepository.findById(wallet)).thenReturn(Optional.of(existing));
        when(miningAccumulatorRepository.save(any(MiningAccumulator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MineResponse response = miningService.mine(wallet);
        assertEquals(2, response.getClickCount());
    }

    @Test
    void mine_whenDisabled_throwsIllegalStateException() {
        ReflectionTestUtils.setField(miningService, "miningEnabled", false);

        assertThrows(IllegalStateException.class, () -> miningService.mine("addr_1"));
        verify(miningAccumulatorRepository, never()).save(any());
    }

    @Test
    void flushAccumulator_createsOneCoinbaseTxPerWallet() {
        MiningAccumulator row1 = new MiningAccumulator("addr_1");
        row1.setClickCount(3);
        MiningAccumulator row2 = new MiningAccumulator("addr_2");
        row2.setClickCount(5);

        when(transactionRepository.sumConfirmedCoinbaseSupply(Transaction.Status.CONFIRMED))
                .thenReturn(BigDecimal.ZERO);

        miningService.flushAccumulator(List.of(row1, row2));

        verify(transactionService).createCoinbaseTransaction(
                eq("addr_1"), eq(new BigDecimal("3.00000000")));
        verify(transactionService).createCoinbaseTransaction(
                eq("addr_2"), eq(new BigDecimal("5.00000000")));
        verify(miningAccumulatorRepository, times(2)).delete(any(MiningAccumulator.class));
    }

    @Test
    void flushAccumulator_withHalvingEnabled_scalesRewardDown() {
        ReflectionTestUtils.setField(miningService, "halvingEnabled", true);
        ReflectionTestUtils.setField(miningService, "halvingThreshold", new BigDecimal("100.00000000"));

        MiningAccumulator row = new MiningAccumulator("addr_1");
        row.setClickCount(10);

        // Supply equals threshold → scale factor = 1 / (1 + 1) = 0.5
        // Expected reward = 10 clicks * 1.0 base * 0.5 = 5.0
        when(transactionRepository.sumConfirmedCoinbaseSupply(Transaction.Status.CONFIRMED))
                .thenReturn(new BigDecimal("100.00000000"));

        miningService.flushAccumulator(List.of(row));

        verify(transactionService).createCoinbaseTransaction(
                eq("addr_1"), eq(new BigDecimal("5.00000000")));
    }

    @Test
    void flushAccumulator_withHalvingEnabled_rewardApproachesZeroAtHugeSupply() {
        ReflectionTestUtils.setField(miningService, "halvingEnabled", true);
        ReflectionTestUtils.setField(miningService, "halvingThreshold", new BigDecimal("100.00000000"));

        MiningAccumulator row = new MiningAccumulator("addr_1");
        row.setClickCount(1);

        // Supply = 1,000,000x threshold → scale = 1 / (1 + 10000) ≈ 0.0001
        // Reward = 1 * 1.0 * ~0.0001 — must be positive but very small
        when(transactionRepository.sumConfirmedCoinbaseSupply(Transaction.Status.CONFIRMED))
                .thenReturn(new BigDecimal("1000000.00000000"));

        miningService.flushAccumulator(List.of(row));

        ArgumentCaptor<BigDecimal> rewardCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactionService).createCoinbaseTransaction(eq("addr_1"), rewardCaptor.capture());
        BigDecimal reward = rewardCaptor.getValue();
        assertTrue(reward.compareTo(BigDecimal.ZERO) > 0, "reward must remain positive");
        assertTrue(reward.compareTo(new BigDecimal("0.01")) < 0, "reward must be very small at huge supply");
    }

    @Test
    void flushAccumulator_emptyList_doesNothing() {
        miningService.flushAccumulator(List.of());

        verify(transactionService, never()).createCoinbaseTransaction(any(), any());
    }

    @Test
    void getStats_returnsConfirmedCoinbaseTotals() {
        String wallet = "addr_1";
        when(transactionRepository.sumConfirmedCoinbaseByReceiver(wallet, Transaction.Status.CONFIRMED))
                .thenReturn(new BigDecimal("42.00000000"));
        when(transactionRepository.countConfirmedCoinbaseByReceiver(wallet, Transaction.Status.CONFIRMED))
                .thenReturn(7L);
        when(miningAccumulatorRepository.findById(wallet)).thenReturn(Optional.empty());

        MiningStatsResponse stats = miningService.getStats(wallet);

        assertNotNull(stats);
        assertEquals(new BigDecimal("42.00000000"), stats.getTotalMined());
        assertEquals(7L, stats.getTotalCount());
        assertEquals(0L, stats.getCooldownRemaining());
    }

    @Test
    void getStats_withActiveCooldown_returnsRemainingSeconds() {
        String wallet = "addr_1";
        MiningAccumulator existing = new MiningAccumulator(wallet);
        existing.setLastMinedAt(LocalDateTime.now().minusSeconds(2));

        when(transactionRepository.sumConfirmedCoinbaseByReceiver(wallet, Transaction.Status.CONFIRMED))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countConfirmedCoinbaseByReceiver(wallet, Transaction.Status.CONFIRMED))
                .thenReturn(0L);
        when(miningAccumulatorRepository.findById(wallet)).thenReturn(Optional.of(existing));

        MiningStatsResponse stats = miningService.getStats(wallet);

        assertTrue(stats.getCooldownRemaining() > 0);
        assertTrue(stats.getCooldownRemaining() <= 5);
    }
}
