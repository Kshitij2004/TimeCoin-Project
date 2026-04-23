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
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Handles TimeCoin purchases against the shared backend schema.
 *
 * Buy/sell transactions are enqueued via MempoolService as PENDING and
 * transitioned to CONFIRMED by BlockAssemblyScheduler. This ensures every
 * coin buy/sell appears in a block on the blockchain explorer.
 */
@Service
public class PurchaseService {

    private static final String DEFAULT_SYMBOL = "TC";
    private final UserRepository userRepository;
    private final CoinRepository coinRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final BalanceService balanceService;
    private final PriceEngineService priceEngineService;
    private final MempoolService mempoolService;
    private final TransactionValidationService transactionValidationService;

    public PurchaseService(
            UserRepository userRepository,
            CoinRepository coinRepository,
            WalletRepository walletRepository,
            WalletService walletService,
            BalanceService balanceService,
            PriceEngineService priceEngineService,
            MempoolService mempoolService,
            TransactionValidationService transactionValidationService) {
        this.userRepository = userRepository;
        this.coinRepository = coinRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.balanceService = balanceService;
        this.priceEngineService = priceEngineService;
        this.mempoolService = mempoolService;
        this.transactionValidationService = transactionValidationService;
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
        wallet = walletService.ensureWalletIdentity(wallet);

        if (coin.getCirculatingSupply().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Insufficient circulating supply");
        }

        // Circulating supply represents the total TC in the market and is not
        // changed by trades — buying and selling only transfer coin between
        // users and the market maker, they don't mint or burn.
        BigDecimal totalUsd = coin.getCurrentPrice()
                .multiply(amount)
                .setScale(2, RoundingMode.HALF_UP);

        // buys have null sender — nonce not used per-sender for coinbase-style rows
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
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setBlockId(null);

        Transaction savedTransaction = mempoolService.enqueueValidatedTransaction(transaction);

        priceEngineService.recordBuy(amount);

        // optimistic balance — ledger only counts CONFIRMED, so show the user
        // their expected post-confirmation balance right away
        BigDecimal confirmedAvailable = balanceService.getBalance(wallet.getWalletAddress()).getAvailable();
        BigDecimal optimisticBalance = confirmedAvailable.add(amount);

        return new PurchaseResponse(
                "Coin purchase enqueued",
                new PurchaseTransactionDTO(savedTransaction),
                new PurchaseWalletDTO(wallet, optimisticBalance)
        );
    }

    @Transactional
    public PurchaseResponse sellCoin(Integer userId, String symbol, BigDecimal amount) {
        validateInput(userId, symbol, amount);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Coin coin = coinRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coin not found"));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        wallet = walletService.ensureWalletIdentity(wallet);

        BigDecimal available = balanceService.getBalance(wallet.getWalletAddress()).getAvailable();
        if (available.compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Insufficient balance: available " + available + ", requested " + amount);
        }

        // Circulating supply is unchanged on sells for the same reason as buys:
        // coin moves between the user and the market, not into or out of existence.
        BigDecimal totalUsd = coin.getCurrentPrice()
                .multiply(amount)
                .setScale(2, RoundingMode.HALF_UP);

        // sells have a real sender — pick the next sequential nonce for this wallet
        int nonce = Math.toIntExact(
                transactionValidationService.getExpectedNextNonce(wallet.getWalletAddress()));

        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setSymbol(normalizeSymbol(symbol));
        transaction.setSenderAddress(wallet.getWalletAddress());
        transaction.setReceiverAddress(null);
        transaction.setAmount(amount);
        transaction.setTransactionType(Transaction.TransactionType.SELL);
        transaction.setPriceAtTime(coin.getCurrentPrice());
        transaction.setTotalUsd(totalUsd);
        transaction.setFee(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        transaction.setNonce(nonce);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionHash("sell_" + UUID.randomUUID());
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setBlockId(null);

        Transaction savedTransaction = mempoolService.enqueueValidatedTransaction(transaction);

        priceEngineService.recordSell(amount);

        BigDecimal optimisticBalance = available.subtract(amount);

        return new PurchaseResponse(
                "Coin sell enqueued",
                new PurchaseTransactionDTO(savedTransaction),
                new PurchaseWalletDTO(wallet, optimisticBalance)
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
}