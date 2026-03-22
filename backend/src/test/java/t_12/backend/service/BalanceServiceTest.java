package t_12.backend.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.entity.StakingEvent;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.StakingEventRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private StakingEventRepository stakingEventRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private BalanceService balanceService;

    private static final String WALLET = "tc1_test_wallet_abc123";

    @BeforeEach
    void setUp() {
        when(walletRepository.existsByWalletAddress(WALLET)).thenReturn(true);
    }

    //wallet existence

    @Test
    void getBalance_walletNotFound_throwsException() {
        when(walletRepository.existsByWalletAddress("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> balanceService.getBalance("nonexistent"));
    }

    @Test
    void getBalance_walletNotFound_doesNotQueryTransactions() {
        when(walletRepository.existsByWalletAddress("nonexistent")).thenReturn(false);

        try { balanceService.getBalance("nonexistent"); } catch (ResourceNotFoundException ignored) {}

        verify(transactionRepository, never())
                .sumAmountByReceiverAndStatus(anyString(), eq(Transaction.Status.CONFIRMED));
    }

    //zero balance (new wallet)

    @Test
    void getBalance_noTransactions_returnsAllZeros() {
        stubTxSums(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        stubStakingSums(BigDecimal.ZERO, BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertBd(BigDecimal.ZERO, r.getAvailable());
        assertBd(BigDecimal.ZERO, r.getStaked());
        assertBd(BigDecimal.ZERO, r.getTotal());
    }

    @Test
    void getBalance_noTransactions_returnsCorrectAddress() {
        stubTxSums(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        stubStakingSums(BigDecimal.ZERO, BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertEquals(WALLET, r.getWalletAddress());
    }

    //receive only

    @Test
    void getBalance_receiveOnly_availableEqualsReceived() {
        stubTxSums(bd("100"), BigDecimal.ZERO, BigDecimal.ZERO);
        stubStakingSums(BigDecimal.ZERO, BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertBd(bd("100"), r.getAvailable());
        assertBd(bd("100"), r.getTotal());
    }

    //send and receive

    @Test
    void getBalance_sendAndReceive_availableIsNet() {
        stubTxSums(bd("100"), bd("40"), BigDecimal.ZERO);
        stubStakingSums(BigDecimal.ZERO, BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertBd(bd("60"), r.getAvailable());
    }

    //fees

    @Test
    void getBalance_feesDeducted_fromAvailable() {
        stubTxSums(bd("100"), bd("30"), bd("2"));
        stubStakingSums(BigDecimal.ZERO, BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        // 100 - 30 - 2 = 68
        assertBd(bd("68"), r.getAvailable());
    }

    @Test
    void getBalance_feesDeducted_totalEqualsAvailableWhenNoStake() {
        stubTxSums(bd("100"), bd("30"), bd("2"));
        stubStakingSums(BigDecimal.ZERO, BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertBd(r.getAvailable(), r.getTotal());
    }

    //staking

    @Test
    void getBalance_withStaking_stakedSubtractedFromAvailable() {
        stubTxSums(bd("200"), bd("50"), bd("5"));
        stubStakingSums(bd("30"), BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        // 200 - 50 - 5 - 30 = 115
        assertBd(bd("115"), r.getAvailable());
        assertBd(bd("30"), r.getStaked());
    }

    @Test
    void getBalance_withStaking_totalIncludesStaked() {
        stubTxSums(bd("200"), bd("50"), bd("5"));
        stubStakingSums(bd("30"), BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        // total = 115 + 30 = 145
        assertBd(bd("145"), r.getTotal());
    }

    //unstaking

    @Test
    void getBalance_withUnstaking_netStakedReduced() {
        stubTxSums(bd("100"), BigDecimal.ZERO, BigDecimal.ZERO);
        stubStakingSums(bd("50"), bd("20"));

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertBd(bd("30"), r.getStaked());
        // 100 - 0 - 0 - 30 = 70
        assertBd(bd("70"), r.getAvailable());
    }

    @Test
    void getBalance_fullyUnstaked_stakedIsZero() {
        stubTxSums(bd("100"), BigDecimal.ZERO, BigDecimal.ZERO);
        stubStakingSums(bd("50"), bd("50"));

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertBd(BigDecimal.ZERO, r.getStaked());
        assertBd(bd("100"), r.getAvailable());
    }

    //null safety

    @Test
    void getBalance_nullFromRepository_treatedAsZero() {
        when(transactionRepository.sumAmountByReceiverAndStatus(WALLET, Transaction.Status.CONFIRMED)).thenReturn(null);
        when(transactionRepository.sumAmountBySenderAndStatus(WALLET, Transaction.Status.CONFIRMED)).thenReturn(null);
        when(transactionRepository.sumFeesBySenderAndStatus(WALLET, Transaction.Status.CONFIRMED)).thenReturn(null);
        when(stakingEventRepository.sumAmountByWalletAddressAndEventType(WALLET, StakingEvent.EventType.STAKE)).thenReturn(null);
        when(stakingEventRepository.sumAmountByWalletAddressAndEventType(WALLET, StakingEvent.EventType.UNSTAKE)).thenReturn(null);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertNotNull(r);
        assertBd(BigDecimal.ZERO, r.getAvailable());
        assertBd(BigDecimal.ZERO, r.getStaked());
        assertBd(BigDecimal.ZERO, r.getTotal());
    }

    //complex scenario

    @Test
    void getBalance_multipleTransfers_correctNetBalance() {
        // received 500, sent 120, fees 8, staked 100, unstaked 25
        stubTxSums(bd("500"), bd("120"), bd("8"));
        stubStakingSums(bd("100"), bd("25"));

        BalanceResponse r = balanceService.getBalance(WALLET);

        // net staked = 75
        assertBd(bd("75"), r.getStaked());
        // available = 500 - 120 - 8 - 75 = 297
        assertBd(bd("297"), r.getAvailable());
        // total = 297 + 75 = 372
        assertBd(bd("372"), r.getTotal());
    }

    //structural

    @Test
    void getBalance_responseHasAllFields() {
        stubTxSums(bd("50"), bd("10"), bd("1"));
        stubStakingSums(bd("5"), BigDecimal.ZERO);

        BalanceResponse r = balanceService.getBalance(WALLET);

        assertNotNull(r.getWalletAddress());
        assertNotNull(r.getAvailable());
        assertNotNull(r.getStaked());
        assertNotNull(r.getTotal());
    }

    @Test
    void getBalance_totalAlwaysEqualsAvailablePlusStaked() {
        stubTxSums(bd("999.99"), bd("123.45"), bd("6.78"));
        stubStakingSums(bd("200"), bd("50"));

        BalanceResponse r = balanceService.getBalance(WALLET);

        BigDecimal expectedTotal = r.getAvailable().add(r.getStaked());
        assertBd(expectedTotal, r.getTotal());
    }

    //helpers

    private void stubTxSums(BigDecimal received, BigDecimal sent, BigDecimal fees) {
        when(transactionRepository.sumAmountByReceiverAndStatus(WALLET, Transaction.Status.CONFIRMED)).thenReturn(received);
        when(transactionRepository.sumAmountBySenderAndStatus(WALLET, Transaction.Status.CONFIRMED)).thenReturn(sent);
        when(transactionRepository.sumFeesBySenderAndStatus(WALLET, Transaction.Status.CONFIRMED)).thenReturn(fees);
    }

    private void stubStakingSums(BigDecimal staked, BigDecimal unstaked) {
        when(stakingEventRepository.sumAmountByWalletAddressAndEventType(WALLET, StakingEvent.EventType.STAKE)).thenReturn(staked);
        when(stakingEventRepository.sumAmountByWalletAddressAndEventType(WALLET, StakingEvent.EventType.UNSTAKE)).thenReturn(unstaked);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }

    private static void assertBd(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                "Expected " + expected + " but got " + actual);
    }
}