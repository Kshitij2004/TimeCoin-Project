package t_12.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.api.coin.PurchaseResponse;
import t_12.backend.api.coin.PurchaseTransactionDTO;
import t_12.backend.api.coin.PurchaseWalletDTO;
import t_12.backend.entity.Coin;
import t_12.backend.entity.Transaction;
import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.CoinRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Handles TimeCoin purchases against the shared backend schema.
 */
@Service
public class PurchaseService {

    private static final String DEFAULT_SYMBOL = "TC";

    private final UserRepository userRepository;
    private final CoinRepository coinRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public PurchaseService(
            UserRepository userRepository,
            CoinRepository coinRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.coinRepository = coinRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public PurchaseResponse purchaseCoin(Integer userId, String symbol, BigDecimal amount) {
        validateInput(userId, symbol, amount);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Coin coin = coinRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coin not found"));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        ensureWalletIdentity(wallet);

        if (coin.getCirculatingSupply().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Insufficient circulating supply");
        }

        coin.setCirculatingSupply(coin.getCirculatingSupply().subtract(amount));
        coin.setUpdatedAt(LocalDateTime.now());
        coinRepository.save(coin);

        wallet.setCoinBalance(wallet.getCoinBalance().add(amount));
        walletRepository.save(wallet);

        BigDecimal totalUsd = coin.getCurrentPrice()
                .multiply(amount)
                .setScale(2, RoundingMode.HALF_UP);

        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setSymbol(normalizeSymbol(symbol));
        transaction.setSenderAddress(null);
        transaction.setReceiverAddress(wallet.getWalletAddress());
        transaction.setAmount(amount);
        transaction.setTransactionType(Transaction.TransactionType.BUY);
        transaction.setPriceAtTime(coin.getCurrentPrice());
        transaction.setTotalUsd(totalUsd);
        transaction.setFee(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        transaction.setNonce(0);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionHash("buy_" + UUID.randomUUID());
        transaction.setStatus(Transaction.Status.CONFIRMED);
        transaction.setBlockId(null);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return new PurchaseResponse(
                "Coin purchase successful",
                new PurchaseTransactionDTO(savedTransaction),
                new PurchaseWalletDTO(wallet)
        );
    }

    private void validateInput(Integer userId, String symbol, BigDecimal amount) {
        if (userId == null || userId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId must be a positive integer");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount must be a positive number");
        }

        String normalizedSymbol = normalizeSymbol(symbol);
        if (!DEFAULT_SYMBOL.equals(normalizedSymbol)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "symbol must be TC"
            );
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return DEFAULT_SYMBOL;
        }
        return symbol.trim().toUpperCase();
    }

    private void ensureWalletIdentity(Wallet wallet) {
        boolean updated = false;

        if (wallet.getWalletAddress() == null || wallet.getWalletAddress().isBlank()) {
            wallet.setWalletAddress("addr_" + UUID.randomUUID());
            updated = true;
        }

        if (wallet.getPublicKey() == null || wallet.getPublicKey().isBlank()) {
            wallet.setPublicKey("pub_" + UUID.randomUUID() + "_" + UUID.randomUUID());
            updated = true;
        }

        if (updated) {
            walletRepository.save(wallet);
        }
    }
}
