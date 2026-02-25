package com.cs506.cryptopurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cs506.cryptopurchase.dto.PurchaseResponse;
import com.cs506.cryptopurchase.exception.PurchaseException;
import com.cs506.cryptopurchase.model.CoinSnapshot;
import com.cs506.cryptopurchase.model.TransactionSnapshot;
import com.cs506.cryptopurchase.model.WalletSnapshot;
import com.cs506.cryptopurchase.repository.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(purchaseRepository);
    }

    @Test
    void purchaseCoinSucceedsAndReturnsWalletBalance() {
        when(purchaseRepository.userExists(1)).thenReturn(true);
        when(purchaseRepository.lockCoin("TC")).thenReturn(
            Optional.of(new CoinSnapshot("TC", new BigDecimal("66000.00"), new BigDecimal("100.00000000")))
        );
        when(purchaseRepository.findWallet(1, "TC")).thenReturn(
            Optional.of(new WalletSnapshot(1, "TC", new BigDecimal("1.25000000")))
        );
        when(purchaseRepository.insertTransaction(anyInt(), anyString(), any(), any(), any())).thenReturn(42L);
        when(purchaseRepository.findTransaction(anyLong())).thenReturn(
            Optional.of(new TransactionSnapshot(
                42L,
                1,
                "TC",
                "BUY",
                new BigDecimal("1.25000000"),
                new BigDecimal("66000.00"),
                new BigDecimal("82500.00"),
                LocalDateTime.parse("2026-02-23T12:00:00")
            ))
        );

        PurchaseResponse response = purchaseService.purchaseCoin(1, "tc", new BigDecimal("1.25"));

        assertEquals("Coin purchase successful", response.message());
        assertEquals(new BigDecimal("1.25000000"), response.wallet().balance());
        assertEquals("TC", response.transaction().symbol());
        assertEquals(new BigDecimal("66000.00"), response.transaction().priceUsd());
        verify(purchaseRepository).decrementSupply("TC", new BigDecimal("1.25"));
    }

    @Test
    void purchaseCoinReturnsInsufficientSupply() {
        when(purchaseRepository.userExists(1)).thenReturn(true);
        when(purchaseRepository.lockCoin("TC")).thenReturn(
            Optional.of(new CoinSnapshot("TC", new BigDecimal("66000.00"), new BigDecimal("0.10000000")))
        );

        PurchaseException exception = assertThrows(
            PurchaseException.class,
            () -> purchaseService.purchaseCoin(1, "TC", new BigDecimal("0.25"))
        );

        assertEquals(409, exception.getStatusCode());
        assertEquals("Insufficient circulating supply", exception.getMessage());
    }

    @Test
    void purchaseCoinValidatesAmount() {
        PurchaseException exception = assertThrows(
            PurchaseException.class,
            () -> purchaseService.purchaseCoin(1, "TC", BigDecimal.ZERO)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("amount must be a positive number", exception.getMessage());
    }
}
