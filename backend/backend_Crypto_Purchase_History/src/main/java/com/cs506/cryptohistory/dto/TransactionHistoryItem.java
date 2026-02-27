package com.cs506.cryptohistory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryItem(
    String type,
    BigDecimal amount,
    BigDecimal priceAtTime,
    LocalDateTime timestamp
) {
}
