package com.cs506.cryptopurchase.dto;

import java.math.BigDecimal;

public record WalletDto(
    Integer userId,
    String symbol,
    BigDecimal balance
) {
}
