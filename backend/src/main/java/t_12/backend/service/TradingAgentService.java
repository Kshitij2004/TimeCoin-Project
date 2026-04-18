package t_12.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Autonomous trading agents that buy and sell TimeCoin on a schedule
 * to create realistic price movement. Each agent has a behavior profile
 * that determines how often it buys vs sells.
 *
 * Agents use PurchaseService like any normal user, so all validation,
 * balance checks, and price engine updates apply.
 */
@Service
public class TradingAgentService {

    private static final Logger log = LoggerFactory.getLogger(TradingAgentService.class);
    private static final String SYMBOL = "TC";

    private final PurchaseService purchaseService;
    private final BalanceService balanceService;
    private final WalletService walletService;
    private final Random random = new Random();

    @Value("${agents.enabled:false}")
    private boolean agentsEnabled;

    @Value("${agents.min-trade-amount:0.5}")
    private double minTradeAmount;

    @Value("${agents.max-trade-amount:10.0}")
    private double maxTradeAmount;

    private final List<AgentProfile> agents;

    public TradingAgentService(
            PurchaseService purchaseService,
            BalanceService balanceService,
            WalletService walletService) {
        this.purchaseService = purchaseService;
        this.balanceService = balanceService;
        this.walletService = walletService;

        this.agents = List.of(
                new AgentProfile("Bullish-1", 1, 0.70),
                new AgentProfile("Bullish-2", 2, 0.70),
                new AgentProfile("Bearish-1", 3, 0.30),
                new AgentProfile("Bearish-2", 4, 0.30),
                new AgentProfile("Random-1",  5, 0.50),
                new AgentProfile("Random-2",  6, 0.50)
        );
    }

    /**
     * Runs on the configured interval. Each tick, every agent decides
     * whether to buy or sell based on its profile and executes the trade.
     */
    @Scheduled(fixedDelayString = "${agents.trade-interval-ms:30000}")
    public void tick() {
        if (!agentsEnabled) {
            return;
        }

        for (AgentProfile agent : agents) {
            try {
                executeTrade(agent);
            } catch (Exception ex) {
                log.warn("[{}] trade failed: {}", agent.name, ex.getMessage());
            }
        }
    }

    private void executeTrade(AgentProfile agent) {
        BigDecimal amount = randomAmount();
        boolean shouldBuy = random.nextDouble() < agent.buyProbability;

        if (shouldBuy) {
            purchaseService.purchaseCoin(agent.userId, SYMBOL, amount);
            log.info("[{}] bought {} TC", agent.name, amount);
        } else {
            String walletAddress = walletService.getWalletByUserId(agent.userId).getWalletAddress();
            BigDecimal available = balanceService.getBalance(walletAddress).getAvailable();

            if (available.compareTo(amount) < 0) {
                purchaseService.purchaseCoin(agent.userId, SYMBOL, amount);
                log.info("[{}] wanted to sell but insufficient balance ({}), bought {} TC instead",
                        agent.name, available, amount);
                return;
            }

            purchaseService.sellCoin(agent.userId, SYMBOL, amount);
            log.info("[{}] sold {} TC", agent.name, amount);
        }
    }

    private BigDecimal randomAmount() {
        double range = maxTradeAmount - minTradeAmount;
        double value = minTradeAmount + (random.nextDouble() * range);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static class AgentProfile {
        final String name;
        final int userId;
        final double buyProbability;

        AgentProfile(String name, int userId, double buyProbability) {
            this.name = name;
            this.userId = userId;
            this.buyProbability = buyProbability;
        }
    }
}