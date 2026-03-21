package t_12.backend.api.coin;

import java.math.BigDecimal;

/**
 * Request body for TimeCoin purchases.
 */
public class PurchaseRequest {

    private Integer userId;
    private String symbol;
    private BigDecimal amount;

    public Integer getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
