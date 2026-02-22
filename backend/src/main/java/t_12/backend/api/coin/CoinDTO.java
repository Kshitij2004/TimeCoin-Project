package t_12.backend.api.coin;

import t_12.backend.entity.Coin;
import java.math.BigDecimal;

public class CoinDTO {

    private BigDecimal currentPrice;
    private BigDecimal totalSupply;
    private BigDecimal circulatingSupply;

    public CoinDTO(Coin coin) {
        this.currentPrice = coin.getCurrentPrice();
        this.totalSupply = coin.getTotalSupply();
        this.circulatingSupply = coin.getCirculatingSupply();
    }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getTotalSupply() { return totalSupply; }
    public BigDecimal getCirculatingSupply() { return circulatingSupply; }
}