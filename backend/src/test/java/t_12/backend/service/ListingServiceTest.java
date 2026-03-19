package t_12.backend.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Listing;
import t_12.backend.entity.Transaction;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.ListingRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Unit tests for ListingService.purchaseListing().
 *
 * Uses Mockito to mock all repository dependencies so no database
 * connection is required.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ListingService listingService;

    private Listing activeListing;
    private Wallet buyerWallet;
    private Wallet sellerWallet;

    private static final Integer LISTING_ID   = 1;
    private static final Integer BUYER_ID     = 10;
    private static final Integer SELLER_ID    = 20;
    private static final BigDecimal PRICE     = new BigDecimal("50.00000000");
    private static final BigDecimal SUFFICIENT_BALANCE  = new BigDecimal("100.00000000");
    private static final BigDecimal INSUFFICIENT_BALANCE = new BigDecimal("10.00000000");

    @BeforeEach
    void setUp() {
        // Build a standard ACTIVE listing owned by SELLER_ID
        activeListing = new Listing();
        activeListing.setId(LISTING_ID);
        activeListing.setSellerId(SELLER_ID);
        activeListing.setTitle("Test Listing");
        activeListing.setPrice(PRICE);
        activeListing.setStatus(Listing.Status.ACTIVE);

        // Build buyer wallet with enough balance
        buyerWallet = new Wallet();
        buyerWallet.setId(1);
        buyerWallet.setUserId(BUYER_ID);
        buyerWallet.setWalletAddress("buyer-wallet-address");
        buyerWallet.setCoinBalance(SUFFICIENT_BALANCE);

        // Build seller wallet
        sellerWallet = new Wallet();
        sellerWallet.setId(2);
        sellerWallet.setUserId(SELLER_ID);
        sellerWallet.setWalletAddress("seller-wallet-address");
        sellerWallet.setCoinBalance(BigDecimal.ZERO);
    }

    /**
     * A buyer with sufficient balance should be able to purchase an active listing.
     * Verifies that:
     * - A transaction hash is returned
     * - Buyer balance is debited
     * - Seller balance is credited
     * - A PENDING transaction is saved
     * - The listing is marked SOLD
     */
    @Test
    void purchaseListing_success() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByUserId(SELLER_ID)).thenReturn(Optional.of(sellerWallet));

        String txHash = listingService.purchaseListing(LISTING_ID, BUYER_ID);

        // Transaction hash must be returned
        assertNotNull(txHash);

        // Buyer should be debited
        assertEquals(SUFFICIENT_BALANCE.subtract(PRICE), buyerWallet.getCoinBalance());

        // Seller should be credited
        assertEquals(PRICE, sellerWallet.getCoinBalance());

        // Listing should be marked SOLD
        assertEquals(Listing.Status.SOLD, activeListing.getStatus());

        // A PENDING transaction should be saved
        verify(transactionRepository, times(1)).save(any(Transaction.class));

        // Both wallets should be saved
        verify(walletRepository, times(1)).save(buyerWallet);
        verify(walletRepository, times(1)).save(sellerWallet);

        // Listing should be saved
        verify(listingRepository, times(1)).save(activeListing);
    }

    /**
     * Should throw ResourceNotFoundException when the listing does not exist.
     */
    @Test
    void purchaseListing_listingNotFound_throwsResourceNotFoundException() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        // No wallets or transactions should be touched
        verify(walletRepository, never()).findByUserId(any());
        verify(transactionRepository, never()).save(any());
    }

    /**
     * Should throw IllegalStateException when the listing is already SOLD.
     */
    @Test
    void purchaseListing_listingAlreadySold_throwsIllegalStateException() {
        activeListing.setStatus(Listing.Status.SOLD);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        assertEquals("Listing is no longer available (status: SOLD)", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    /**
     * Should throw IllegalStateException when the listing has been REMOVED.
     */
    @Test
    void purchaseListing_listingRemoved_throwsIllegalStateException() {
        activeListing.setStatus(Listing.Status.REMOVED);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        assertEquals("Listing is no longer available (status: REMOVED)", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    // Self-purchase prevention

    /**
     * Should throw IllegalStateException when the buyer is also the seller.
     */
    @Test
    void purchaseListing_buyerIsSeller_throwsIllegalStateException() {
        // Set the listing's seller to be the same as the buyer
        activeListing.setSellerId(BUYER_ID);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        assertEquals("You cannot purchase your own listing", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    // Wallet validation

    /**
     * Should throw ResourceNotFoundException when the buyer has no wallet.
     */
    @Test
    void purchaseListing_buyerWalletNotFound_throwsResourceNotFoundException() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        verify(transactionRepository, never()).save(any());
    }

    /**
     * Should throw ResourceNotFoundException when the seller has no wallet.
     */
    @Test
    void purchaseListing_sellerWalletNotFound_throwsResourceNotFoundException() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByUserId(SELLER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        verify(transactionRepository, never()).save(any());
    }

    /**
     * Should throw IllegalStateException when the buyer does not have enough TimeCoin.
     * The error message should include both the required and available amounts.
     */
    @Test
    void purchaseListing_insufficientBalance_throwsIllegalStateException() {
        buyerWallet.setCoinBalance(INSUFFICIENT_BALANCE);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));

        // Error message should mention both required and available balance
        assertEquals(
                "Insufficient balance. Required: " + PRICE
                        + ", Available: " + INSUFFICIENT_BALANCE,
                ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    /**
     * Should succeed when the buyer's balance is exactly equal to the listing price.
     * Edge case — balance == price should be allowed.
     */
    @Test
    void purchaseListing_balanceExactlyEqualToPrice_succeeds() {
        buyerWallet.setCoinBalance(PRICE);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByUserId(SELLER_ID)).thenReturn(Optional.of(sellerWallet));

        String txHash = listingService.purchaseListing(LISTING_ID, BUYER_ID);

        assertNotNull(txHash);
        assertEquals(0, buyerWallet.getCoinBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, sellerWallet.getCoinBalance().compareTo(PRICE));
        assertEquals(Listing.Status.SOLD, activeListing.getStatus());
    }
}