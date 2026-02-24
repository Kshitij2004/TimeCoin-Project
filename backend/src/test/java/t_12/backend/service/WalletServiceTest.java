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

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    void GetWalletByUserId_ReturnsWallet_WhenFoundTest() {
        Wallet wallet = new Wallet();
        wallet.setUserId(1L);
        wallet.setCoinBalance(new BigDecimal("5.00000000"));

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByUserId(1L);

        assertEquals(1L, result.getUserId());
        assertEquals(new BigDecimal("5.00000000"), result.getCoinBalance());
        verify(walletRepository, times(1)).findByUserId(1L);
    }

    @Test
    void GetWalletByUserId_ThrowsException_WhenNotFoundTest() {
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            walletService.getWalletByUserId(99L);
        });

        verify(walletRepository, times(1)).findByUserId(99L);
    }
}