package t_12.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.api.coin.PurchaseResponse;
import t_12.backend.entity.Wallet;

@ExtendWith(MockitoExtension.class)
class TradingAgentServiceTest {

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private WalletService walletService;

    private TradingAgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new TradingAgentService(purchaseService, balanceService, walletService);
        ReflectionTestUtils.setField(agentService, "agentsEnabled", true);
        ReflectionTestUtils.setField(agentService, "minTradeAmount", 1.0);
        ReflectionTestUtils.setField(agentService, "maxTradeAmount", 5.0);

        for (int userId = 1; userId <= 6; userId++) {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setWalletAddress("addr_" + userId);
            when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        }

        when(balanceService.getBalance(any())).thenReturn(
                new BalanceResponse("addr_test", new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000")));

        when(purchaseService.purchaseCoin(any(), eq("TC"), any())).thenReturn(mockResponse("buy"));
        when(purchaseService.sellCoin(any(), eq("TC"), any())).thenReturn(mockResponse("sell"));
    }

    @Test
    void tick_agentsDisabled_noTrades() {
        ReflectionTestUtils.setField(agentService, "agentsEnabled", false);

        agentService.tick();

        verify(purchaseService, never()).purchaseCoin(any(), any(), any());
        verify(purchaseService, never()).sellCoin(any(), any(), any());
    }

    @Test
    void tick_agentsEnabled_allAgentsTrade() {
        for (int i = 0; i < 20; i++) {
            agentService.tick();
        }

        // every agent should have traded at least once across 20 ticks
        for (int userId = 1; userId <= 6; userId++) {
            verify(purchaseService, atLeast(1)).purchaseCoin(eq(userId), eq("TC"), any());
        }
    }

    @Test
    void tick_insufficientSellBalance_fallsToBuy() {
        // bearish agent (userId 3) has no balance
        when(balanceService.getBalance("addr_3")).thenReturn(
                new BalanceResponse("addr_3", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        for (int i = 0; i < 20; i++) {
            agentService.tick();
        }

        // bearish agent should still buy since sell fails balance check
        verify(purchaseService, atLeast(1)).purchaseCoin(eq(3), eq("TC"), any());
    }

    @Test
    void tick_tradeFailure_doesNotStopOtherAgents() {
        when(purchaseService.purchaseCoin(eq(1), any(), any()))
                .thenThrow(new RuntimeException("simulated failure"));

        agentService.tick();

        // other agents should still trade despite agent 1 failing
        verify(purchaseService, atLeast(1)).purchaseCoin(eq(2), eq("TC"), any());
    }

    private PurchaseResponse mockResponse(String message) {
        return new PurchaseResponse(message, null, null);
    }
}