package com.cs506.cryptopurchase.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
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
