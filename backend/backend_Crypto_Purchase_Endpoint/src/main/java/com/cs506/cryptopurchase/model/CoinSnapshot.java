package com.cs506.cryptopurchase.model;

import java.math.BigDecimal;

public record CoinSnapshot(
    String symbol,
    BigDecimal currentPriceUsd,
    BigDecimal circulatingSupply
) {
}
