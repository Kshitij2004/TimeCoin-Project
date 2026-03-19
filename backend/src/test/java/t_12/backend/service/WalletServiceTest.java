package t_12.backend.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.WalletRepository;

/**
 * Unit tests for WalletService class. Tests wallet lookup and keypair
 * generation behavior.
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
     * Tests that getWalletByAddress returns the wallet when found.
     */
    @Test
    void GetWalletByAddress_ReturnsWallet_WhenFoundTest() {
        Wallet wallet = new Wallet();
        wallet.setWalletAddress("wlt_lookup");

        when(walletRepository.findByWalletAddress("wlt_lookup")).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByAddress("wlt_lookup");

        assertEquals("wlt_lookup", result.getWalletAddress());
        verify(walletRepository, times(1)).findByWalletAddress("wlt_lookup");
    }

    /**
     * Tests that getWalletByPublicKey returns the wallet when found.
     */
    @Test
    void GetWalletByPublicKey_ReturnsWallet_WhenFoundTest() {
        Wallet wallet = new Wallet();
        wallet.setPublicKey("pub_lookup");

        when(walletRepository.findByPublicKey("pub_lookup")).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByPublicKey("pub_lookup");

        assertEquals("pub_lookup", result.getPublicKey());
        verify(walletRepository, times(1)).findByPublicKey("pub_lookup");
    }

    /**
     * Tests that createWalletForUser generates key material and a derived
     * address for users without an existing wallet.
     */
    @Test
    void CreateWalletForUser_ReturnsWalletWithGeneratedIdentity_WhenUserHasNoWalletTest() {
        when(walletRepository.existsByUserId(101)).thenReturn(false);
        when(walletRepository.existsByPublicKey(anyString())).thenReturn(false);
        when(walletRepository.existsByWalletAddress(anyString())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet createdWallet = invocation.getArgument(0);
            createdWallet.setId(1);
            return createdWallet;
        });

        WalletCreationResult result = walletService.createWalletForUser(101);

        assertEquals(101, result.getWallet().getUserId());
        assertEquals(new BigDecimal("0E-8"), result.getWallet().getCoinBalance());
        assertNotNull(result.getWallet().getPublicKey());
        assertNotNull(result.getWallet().getWalletAddress());
        assertNotNull(result.getPrivateKey());
        assertEquals(
                result.getWallet().getWalletAddress(),
                walletService.deriveWalletAddressFromPublicKey(result.getWallet().getPublicKey())
        );
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    /**
     * Tests that createWalletForUser rejects users that already have a wallet.
     */
    @Test
    void CreateWalletForUser_ThrowsException_WhenWalletAlreadyExistsTest() {
        when(walletRepository.existsByUserId(200)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            walletService.createWalletForUser(200);
        });

        verify(walletRepository, times(1)).existsByUserId(200);
    }

    /**
     * Tests that createWalletForUser produces unique keypairs and addresses
     * across wallet creation calls.
     */
    @Test
    void CreateWalletForUser_GeneratesUniqueIdentity_ForDifferentUsersTest() {
        AtomicInteger idSequence = new AtomicInteger(1);
        when(walletRepository.existsByUserId(1)).thenReturn(false);
        when(walletRepository.existsByUserId(2)).thenReturn(false);
        when(walletRepository.existsByPublicKey(anyString())).thenReturn(false);
        when(walletRepository.existsByWalletAddress(anyString())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet createdWallet = invocation.getArgument(0);
            createdWallet.setId(idSequence.getAndIncrement());
            return createdWallet;
        });

        WalletCreationResult firstWalletResult = walletService.createWalletForUser(1);
        WalletCreationResult secondWalletResult = walletService.createWalletForUser(2);

        assertNotEquals(
                firstWalletResult.getWallet().getPublicKey(),
                secondWalletResult.getWallet().getPublicKey()
        );
        assertNotEquals(
                firstWalletResult.getWallet().getWalletAddress(),
                secondWalletResult.getWallet().getWalletAddress()
        );
        assertNotEquals(firstWalletResult.getPrivateKey(), secondWalletResult.getPrivateKey());
    }

    /**
     * Tests that ensureWalletIdentity backfills key and address when missing.
     */
    @Test
    void EnsureWalletIdentity_BackfillsIdentity_WhenMissingTest() {
        Wallet wallet = new Wallet();
        wallet.setId(11);
        wallet.setUserId(11);
        wallet.setCoinBalance(BigDecimal.ZERO);

        when(walletRepository.existsByPublicKey(anyString())).thenReturn(false);
        when(walletRepository.existsByWalletAddress(anyString())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.ensureWalletIdentity(wallet);

        assertNotNull(result.getPublicKey());
        assertNotNull(result.getWalletAddress());
        assertEquals(
                result.getWalletAddress(),
                walletService.deriveWalletAddressFromPublicKey(result.getPublicKey())
        );
        verify(walletRepository, times(1)).save(wallet);
    }
}
