package com.cs506.cryptopurchase.model;

import java.math.BigDecimal;

public record WalletSnapshot(
    Integer userId,
    String symbol,
    BigDecimal balance
) {
}
