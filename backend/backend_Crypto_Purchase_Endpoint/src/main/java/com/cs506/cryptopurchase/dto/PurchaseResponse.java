package com.cs506.cryptopurchase.dto;

public record PurchaseResponse(
    String message,
    TransactionDto transaction,
    WalletDto wallet
) {
}
