package com.cs506.cryptohistory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRow(
    String transactionType,
    BigDecimal quantity,
    BigDecimal priceUsd,
    LocalDateTime createdAt
) {
}
