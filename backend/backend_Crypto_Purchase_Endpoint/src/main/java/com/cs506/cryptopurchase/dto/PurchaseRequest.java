package com.cs506.cryptopurchase.dto;

import java.math.BigDecimal;

public record PurchaseRequest(
    Integer userId,
    String symbol,
    BigDecimal amount
) {
}
