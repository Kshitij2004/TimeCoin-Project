package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import t_12.backend.api.coin.PurchaseResponse;
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
 * Unit tests for PurchaseService.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoinRepository coinRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    @Test
    void purchaseCoinSucceedsAndReturnsUpdatedWallet() {
        User user = new User();
        user.setId(1);

        Coin coin = new Coin();
        coin.setId(1L);
        coin.setCurrentPrice(new BigDecimal("66000.00"));
        coin.setCirculatingSupply(new BigDecimal("100.00"));
        coin.setTotalSupply(new BigDecimal("1000000.00"));

        Wallet wallet = new Wallet();
        wallet.setUserId(1);
        wallet.setWalletAddress("wlt_1");
        wallet.setPublicKey("pub_1");
        wallet.setCoinBalance(BigDecimal.ZERO);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));
        when(walletRepository.findByUserId(1)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(42);
            return transaction;
        });

        PurchaseResponse response = purchaseService.purchaseCoin(1, "tc", new BigDecimal("1.25"));

        assertEquals("Coin purchase successful", response.getMessage());
        assertEquals(new BigDecimal("1.25"), response.getTransaction().getAmount());
        assertEquals(new BigDecimal("1.25"), response.getWallet().getCoinBalance());
        assertEquals(new BigDecimal("98.75"), coin.getCirculatingSupply());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(Transaction.TransactionType.BUY, transactionCaptor.getValue().getTransactionType());
    }

    @Test
    void purchaseCoinRejectsUnknownSymbol() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> purchaseService.purchaseCoin(1, "ETH", new BigDecimal("1.00"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("symbol must be TC", exception.getMessage());
    }

    @Test
    void purchaseCoinRejectsInsufficientSupply() {
        User user = new User();
        user.setId(1);

        Coin coin = new Coin();
        coin.setCurrentPrice(new BigDecimal("66000.00"));
        coin.setCirculatingSupply(new BigDecimal("0.10"));

        Wallet wallet = new Wallet();
        wallet.setUserId(1);
        wallet.setWalletAddress("wlt_1");
        wallet.setPublicKey("pub_1");
        wallet.setCoinBalance(BigDecimal.ZERO);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));
        when(walletRepository.findByUserId(1)).thenReturn(Optional.of(wallet));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> purchaseService.purchaseCoin(1, "TC", new BigDecimal("0.25"))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Insufficient circulating supply", exception.getMessage());
    }

    @Test
    void purchaseCoinBackfillsMissingWalletIdentity() {
        User user = new User();
        user.setId(9);

        Coin coin = new Coin();
        coin.setId(1L);
        coin.setCurrentPrice(new BigDecimal("10.00"));
        coin.setCirculatingSupply(new BigDecimal("500000.00"));
        coin.setTotalSupply(new BigDecimal("1000000.00"));

        Wallet wallet = new Wallet();
        wallet.setUserId(9);
        wallet.setCoinBalance(BigDecimal.ZERO);

        when(userRepository.findById(9)).thenReturn(Optional.of(user));
        when(coinRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(coin));
        when(walletRepository.findByUserId(9)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(99);
            return transaction;
        });

        PurchaseResponse response = purchaseService.purchaseCoin(9, "TC", new BigDecimal("1.25"));

        assertNotNull(response.getWallet().getWalletAddress());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(walletRepository, atLeastOnce()).save(wallet);
        assertNotNull(transactionCaptor.getValue().getReceiverAddress());
    }
}
