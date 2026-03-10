package t_12.backend.api.coin;

import java.math.BigDecimal;

import t_12.backend.entity.Coin;

/**
 * Data Transfer Object for Coin information.
 */
public class CoinDTO {

    private final BigDecimal currentPrice;
    private final BigDecimal totalSupply;
    private final BigDecimal circulatingSupply;

    /**
     * Constructs a CoinDTO from a Coin entity.
     *
     * @param coin the Coin entity to convert
     */
    public CoinDTO(Coin coin) {
        this.currentPrice = coin.getCurrentPrice();
        this.totalSupply = coin.getTotalSupply();
        this.circulatingSupply = coin.getCirculatingSupply();
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getTotalSupply() {
        return totalSupply;
    }

    public BigDecimal getCirculatingSupply() {
        return circulatingSupply;
    }
}
