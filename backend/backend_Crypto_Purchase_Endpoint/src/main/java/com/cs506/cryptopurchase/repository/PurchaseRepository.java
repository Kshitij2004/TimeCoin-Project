package com.cs506.cryptopurchase.repository;

import com.cs506.cryptopurchase.model.CoinSnapshot;
import com.cs506.cryptopurchase.model.TransactionSnapshot;
import com.cs506.cryptopurchase.model.WalletSnapshot;
import java.math.BigDecimal;
import java.util.Optional;

public interface PurchaseRepository {
    boolean userExists(int userId);
    Optional<CoinSnapshot> lockCoin(String symbol);
    void decrementSupply(String symbol, BigDecimal amount);
    void upsertWallet(int userId, String symbol, BigDecimal amount);
    Optional<WalletSnapshot> findWallet(int userId, String symbol);
    long insertTransaction(int userId, String symbol, BigDecimal amount, BigDecimal priceUsd, BigDecimal totalUsd);
    Optional<TransactionSnapshot> findTransaction(long id);
}
