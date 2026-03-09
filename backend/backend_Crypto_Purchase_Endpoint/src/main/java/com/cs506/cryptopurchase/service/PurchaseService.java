package com.cs506.cryptopurchase.service;

import com.cs506.cryptopurchase.dto.PurchaseResponse;
import com.cs506.cryptopurchase.dto.TransactionDto;
import com.cs506.cryptopurchase.dto.WalletDto;
import com.cs506.cryptopurchase.exception.PurchaseException;
import com.cs506.cryptopurchase.model.CoinSnapshot;
import com.cs506.cryptopurchase.model.TransactionSnapshot;
import com.cs506.cryptopurchase.model.WalletSnapshot;
import com.cs506.cryptopurchase.repository.PurchaseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;

    public PurchaseService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional
    public PurchaseResponse purchaseCoin(Integer userId, String symbol, BigDecimal amount) {
        validateInput(userId, symbol, amount);
        String normalizedSymbol = symbol.trim().toUpperCase();

        if (!purchaseRepository.userExists(userId)) {
            throw new PurchaseException("User not found", 404);
        }

        CoinSnapshot coin = purchaseRepository.lockCoin(normalizedSymbol)
            .orElseThrow(() -> new PurchaseException("Coin not found", 404));

        if (coin.circulatingSupply().compareTo(amount) < 0) {
            throw new PurchaseException("Insufficient circulating supply", 409);
        }

        purchaseRepository.decrementSupply(normalizedSymbol, amount);
        purchaseRepository.upsertWallet(userId, normalizedSymbol, amount);

        WalletSnapshot wallet = purchaseRepository.findWallet(userId, normalizedSymbol)
            .orElseThrow(() -> new PurchaseException("Wallet not found after purchase", 500));

        BigDecimal totalUsd = coin.currentPriceUsd()
            .multiply(amount)
            .setScale(2, RoundingMode.HALF_UP);

        long transactionId = purchaseRepository.insertTransaction(
            userId,
            normalizedSymbol,
            amount,
            coin.currentPriceUsd(),
            totalUsd
        );

        TransactionSnapshot transaction = purchaseRepository.findTransaction(transactionId)
            .orElseThrow(() -> new PurchaseException("Transaction not found after insert", 500));

        return new PurchaseResponse(
            "Coin purchase successful",
            new TransactionDto(
                transaction.id(),
                transaction.userId(),
                transaction.symbol(),
                transaction.transactionType(),
                transaction.quantity(),
                transaction.priceUsd(),
                transaction.totalUsd(),
                transaction.createdAt()
            ),
            new WalletDto(
                wallet.userId(),
                wallet.symbol(),
                wallet.balance()
            )
        );
    }

    private void validateInput(Integer userId, String symbol, BigDecimal amount) {
        if (userId == null || userId <= 0) {
            throw new PurchaseException("userId must be a positive integer", 400);
        }

        if (symbol == null || symbol.trim().isEmpty() || symbol.trim().length() > 10) {
            throw new PurchaseException("symbol must be a non-empty string up to 10 chars", 400);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("amount must be a positive number", 400);
        }
    }
}
