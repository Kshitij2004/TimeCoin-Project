package t_12.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.entity.Coin;
import t_12.backend.entity.PriceHistory;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.CoinRepository;
import t_12.backend.repository.PriceHistoryRepository;

/**
 * Recalculates TimeCoin price after each trade based on buy/sell volume.
 *
 * Formula:
 *   priceChange = sensitivity * (tradeAmount / circulatingSupply) * currentPrice
 *   newPrice = clamp(currentPrice +/- priceChange, FLOOR, CEILING)
 *
 * Buys push the price up, sells push it down.
 */
@Service
public class PriceEngineService {

    private static final BigDecimal PRICE_FLOOR = new BigDecimal("0.01");
    private static final BigDecimal PRICE_CEILING = new BigDecimal("1000000.00");

    private final CoinRepository coinRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Value("${price-engine.sensitivity:5.0}")
    private BigDecimal sensitivity;

    public PriceEngineService(CoinRepository coinRepository,
                              PriceHistoryRepository priceHistoryRepository) {
        this.coinRepository = coinRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional
    public void recordBuy(BigDecimal tradeAmount) {
        recalculate(tradeAmount, true);
    }

    @Transactional
    public void recordSell(BigDecimal tradeAmount) {
        recalculate(tradeAmount, false);
    }

    private void recalculate(BigDecimal tradeAmount, boolean isBuy) {
        Coin coin = coinRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("No coin data found"));

        BigDecimal currentPrice = coin.getCurrentPrice();
        BigDecimal supply = coin.getCirculatingSupply();

        if (supply.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // keep high precision throughout the multiplication chain
        BigDecimal ratio = tradeAmount.divide(supply, 10, RoundingMode.HALF_UP);
        BigDecimal priceChange = sensitivity.multiply(ratio).multiply(currentPrice);

        BigDecimal newPrice;
        if (isBuy) {
            newPrice = currentPrice.add(priceChange);
        } else {
            newPrice = currentPrice.subtract(priceChange);
        }

        // round only at the end, after add/subtract
        newPrice = newPrice.setScale(2, RoundingMode.HALF_UP);

        if (newPrice.compareTo(PRICE_FLOOR) < 0) {
            newPrice = PRICE_FLOOR;
        }
        if (newPrice.compareTo(PRICE_CEILING) > 0) {
            newPrice = PRICE_CEILING;
        }

        LocalDateTime now = LocalDateTime.now();
        coin.setCurrentPrice(newPrice);
        coin.setUpdatedAt(now);
        coinRepository.save(coin);

        priceHistoryRepository.save(new PriceHistory(newPrice, now));
    }
}