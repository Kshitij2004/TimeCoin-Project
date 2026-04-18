package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.api.staking.StakingOverviewResponse;
import t_12.backend.entity.StakingEvent;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.StakingEventRepository;

@ExtendWith(MockitoExtension.class)
class StakingServiceTest {

    private static final String WALLET = "wlt_test_staking_001";

    @Mock
    private StakingEventRepository stakingEventRepository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private StakingService stakingService;

    @Test
    void stake_recordsStakeEventAndReturnsUpdatedOverview() {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress(WALLET);

        when(walletService.getWalletByAddress(WALLET)).thenReturn(wallet);
        when(balanceService.getBalance(WALLET))
                .thenReturn(balance("100.00000000", "20.00000000", "120.00000000"))
                .thenReturn(balance("90.00000000", "30.00000000", "120.00000000"));

        StakingEvent saved = event(100, StakingEvent.EventType.STAKE, "10.00000000");
        when(stakingEventRepository.save(any(StakingEvent.class))).thenReturn(saved);
        when(stakingEventRepository.findByWalletAddressOrderByCreatedAtDescIdDesc(WALLET))
                .thenReturn(List.of(saved));

        StakingOverviewResponse response = stakingService.stake(WALLET, bd("10.00000000"));

        assertEquals(WALLET, response.getWalletAddress());
        assertBd("90.00000000", response.getAvailable());
        assertBd("30.00000000", response.getStaked());
        assertBd("120.00000000", response.getTotal());
        assertEquals(1, response.getEvents().size());
        assertEquals("STAKE", response.getEvents().get(0).getEventType());

        ArgumentCaptor<StakingEvent> captor = ArgumentCaptor.forClass(StakingEvent.class);
        verify(stakingEventRepository).save(captor.capture());
        assertEquals(WALLET, captor.getValue().getWalletAddress());
        assertEquals(StakingEvent.EventType.STAKE, captor.getValue().getEventType());
        assertBd("10.00000000", captor.getValue().getAmount());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void unstake_recordsUnstakeEventAndReturnsUpdatedOverview() {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress(WALLET);

        when(walletService.getWalletByAddress(WALLET)).thenReturn(wallet);
        when(balanceService.getBalance(WALLET))
                .thenReturn(balance("80.00000000", "30.00000000", "110.00000000"))
                .thenReturn(balance("90.00000000", "20.00000000", "110.00000000"));

        StakingEvent saved = event(101, StakingEvent.EventType.UNSTAKE, "10.00000000");
        when(stakingEventRepository.save(any(StakingEvent.class))).thenReturn(saved);
        when(stakingEventRepository.findByWalletAddressOrderByCreatedAtDescIdDesc(WALLET))
                .thenReturn(List.of(saved));

        StakingOverviewResponse response = stakingService.unstake(WALLET, bd("10.00000000"));

        assertEquals(WALLET, response.getWalletAddress());
        assertBd("90.00000000", response.getAvailable());
        assertBd("20.00000000", response.getStaked());
        assertBd("110.00000000", response.getTotal());
        assertEquals(1, response.getEvents().size());
        assertEquals("UNSTAKE", response.getEvents().get(0).getEventType());

        ArgumentCaptor<StakingEvent> captor = ArgumentCaptor.forClass(StakingEvent.class);
        verify(stakingEventRepository).save(captor.capture());
        assertEquals(StakingEvent.EventType.UNSTAKE, captor.getValue().getEventType());
        assertBd("10.00000000", captor.getValue().getAmount());
    }

    @Test
    void stake_insufficientAvailableBalance_rejected() {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress(WALLET);

        when(walletService.getWalletByAddress(WALLET)).thenReturn(wallet);
        when(balanceService.getBalance(WALLET))
                .thenReturn(balance("5.00000000", "20.00000000", "25.00000000"));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> stakingService.stake(WALLET, bd("10.00000000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Insufficient available balance to stake", ex.getMessage());
        verify(stakingEventRepository, never()).save(any());
    }

    @Test
    void unstake_doubleUnstake_rejectedWhenAmountExceedsCurrentStaked() {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress(WALLET);

        when(walletService.getWalletByAddress(WALLET)).thenReturn(wallet);
        when(balanceService.getBalance(WALLET))
                .thenReturn(balance("95.00000000", "5.00000000", "100.00000000"));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> stakingService.unstake(WALLET, bd("10.00000000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Insufficient staked balance to unstake", ex.getMessage());
        verify(stakingEventRepository, never()).save(any());
    }

    @Test
    void getStakingOverview_returnsEventHistoryAndCurrentBalances() {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress(WALLET);

        when(walletService.getWalletByAddress(WALLET)).thenReturn(wallet);
        when(balanceService.getBalance(WALLET))
                .thenReturn(balance("70.00000000", "30.00000000", "100.00000000"));

        StakingEvent newest = event(200, StakingEvent.EventType.UNSTAKE, "5.00000000");
        StakingEvent older = event(199, StakingEvent.EventType.STAKE, "15.00000000");
        when(stakingEventRepository.findByWalletAddressOrderByCreatedAtDescIdDesc(WALLET))
                .thenReturn(List.of(newest, older));

        StakingOverviewResponse response = stakingService.getStakingOverview(WALLET);

        assertEquals(2, response.getEvents().size());
        assertEquals("UNSTAKE", response.getEvents().get(0).getEventType());
        assertEquals("STAKE", response.getEvents().get(1).getEventType());
        assertBd("70.00000000", response.getAvailable());
        assertBd("30.00000000", response.getStaked());
        assertBd("100.00000000", response.getTotal());
    }

    private BalanceResponse balance(String available, String staked, String total) {
        return new BalanceResponse(WALLET, bd(available), bd(staked), bd(total));
    }

    private StakingEvent event(Integer id, StakingEvent.EventType type, String amount) {
        StakingEvent event = new StakingEvent();
        event.setId(id);
        event.setWalletAddress(WALLET);
        event.setEventType(type);
        event.setAmount(bd(amount));
        event.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        return event;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void assertBd(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
