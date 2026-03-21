package t_12.backend.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.WalletRepository;

/**
 * Unit tests for WalletService class. Tests wallet retrieval functionality
 * including success and error cases.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    /**
     * Tests that getWalletByUserId returns the wallet when found.
     */
    @Test
    void GetWalletByUserId_ReturnsWallet_WhenFoundTest() {
        Wallet wallet = new Wallet();
        wallet.setUserId(101);
        wallet.setCoinBalance(new BigDecimal("5.00000000"));

        when(walletRepository.findByUserId(101)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByUserId(101);

        assertEquals(101, result.getUserId());
        assertEquals(new BigDecimal("5.00000000"), result.getCoinBalance());
        verify(walletRepository, times(1)).findByUserId(101);
    }

    /**
     * Tests that getWalletByUserId throws ResourceNotFoundException when wallet
     * is not found.
     */
    @Test
    void GetWalletByUserId_ThrowsException_WhenNotFoundTest() {
        when(walletRepository.findByUserId(111)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            walletService.getWalletByUserId(111);
        });

        verify(walletRepository, times(1)).findByUserId(111);
    }

    /**
     * Tests that getWalletByUserId throws ResourceNotFoundException when wallet
     * is null.
     */
    @Test
    void GetWalletByUserId_ThrowsException_WhenNullTest() {
        when(walletRepository.findByUserId(111)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            walletService.getWalletByUserId(111);
        });

        verify(walletRepository, times(1)).findByUserId(111);
    }

    /**
     * Tests that getWalletBySenderAddress returns the wallet when found.
     */
    @Test
    void getWalletBySenderAddress_returnsWallet_whenFoundTest() {
        Wallet wallet = new Wallet();
        wallet.setUserId(101);
        wallet.setWalletAddress("addr_abc123");
        wallet.setCoinBalance(new BigDecimal("5.00000000"));

        when(walletRepository.findByWalletAddress("addr_abc123"))
                .thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletBySenderAddress("addr_abc123");

        assertEquals("addr_abc123", result.getWalletAddress());
        assertEquals(new BigDecimal("5.00000000"), result.getCoinBalance());
        verify(walletRepository, times(1)).findByWalletAddress("addr_abc123");
    }

    /**
     * Tests that getWalletBySenderAddress throws ResourceNotFoundException when
     * wallet is not found.
     */
    @Test
    void getWalletBySenderAddress_throwsResourceNotFoundException_whenNotFoundTest() {
        when(walletRepository.findByWalletAddress("addr_missing"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, ()
                -> walletService.getWalletBySenderAddress("addr_missing")
        );

        verify(walletRepository, times(1)).findByWalletAddress("addr_missing");
    }
}
