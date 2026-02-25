package com.cs506.cryptopurchase.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionSnapshot(
    Long id,
    Integer userId,
    String symbol,
    String transactionType,
    BigDecimal quantity,
    BigDecimal priceUsd,
    BigDecimal totalUsd,
    LocalDateTime createdAt
) {
}
