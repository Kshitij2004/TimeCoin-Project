package t_12.backend.service;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import t_12.backend.api.listings.CreateListingRequest;
import t_12.backend.entity.Listing;
import t_12.backend.entity.Transaction;
import t_12.backend.entity.Wallet;
import t_12.backend.repository.ListingRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.WalletRepository;
/**
 * Unit tests for ListingService class. Tests listing CRUD operations and
 * authorization checks.
 */
@ExtendWith(MockitoExtension.class)
public class ListingsServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingService listingService;

    @Mock
    private WalletRepository walletRepository;
 
    @Mock
    private TransactionRepository transactionRepository;
 
    @Mock
    private TransactionService transactionService;


    // Information for purchasing tests
 
    private Listing activeListing;
    private Wallet buyerWallet;
    private Wallet sellerWallet;
 
    private static final Integer LISTING_ID = 1;
    private static final Integer BUYER_ID = 10;
    private static final Integer SELLER_ID = 20;
    private static final BigDecimal PRICE = new BigDecimal("50.00000000");
    private static final BigDecimal SUFFICIENT_BALANCE = new BigDecimal("100.00000000");
    private static final BigDecimal INSUFFICIENT_BALANCE = new BigDecimal("10.00000000");
 
    @BeforeEach
    void setUp() {
        activeListing = new Listing();
        activeListing.setId(LISTING_ID);
        activeListing.setSellerId(SELLER_ID);
        activeListing.setTitle("Test Listing");
        activeListing.setPrice(PRICE);
        activeListing.setStatus(Listing.Status.ACTIVE);
 
        buyerWallet = new Wallet();
        buyerWallet.setId(1);
        buyerWallet.setUserId(BUYER_ID);
        buyerWallet.setWalletAddress("buyer-wallet-address");
        buyerWallet.setCoinBalance(SUFFICIENT_BALANCE);
 
        sellerWallet = new Wallet();
        sellerWallet.setId(2);
        sellerWallet.setUserId(SELLER_ID);
        sellerWallet.setWalletAddress("seller-wallet-address");
        sellerWallet.setCoinBalance(BigDecimal.ZERO);
    }

    /**
     * Tests that createListing saves and returns a listing with ACTIVE status
     * and the correct sellerId.
     */
    @Test
    void CreateListing_ReturnsListing_WhenValidInputTest() {
        CreateListingRequest request = new CreateListingRequest();
        request.setTitle("Test Listing");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("5.00000000"));
        request.setCategory("Test Category");
        request.setImageUrl("http://example.com/image.jpg");

        Listing savedListing = new Listing();
        savedListing.setId(1);
        savedListing.setSellerId(9);
        savedListing.setTitle(request.getTitle());
        savedListing.setDescription(request.getDescription());
        savedListing.setPrice(request.getPrice());
        savedListing.setCategory(request.getCategory());
        savedListing.setImageUrl(request.getImageUrl());
        savedListing.setStatus(Listing.Status.ACTIVE);

        when(listingRepository.save(any(Listing.class)))
                .thenReturn(savedListing);

        Listing result = listingService.createListing(request, 9);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(9, result.getSellerId());
        assertEquals("Test Listing", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(new BigDecimal("5.00000000"), result.getPrice());
        assertEquals("Test Category", result.getCategory());
        assertEquals("http://example.com/image.jpg", result.getImageUrl());
        assertEquals(Listing.Status.ACTIVE, result.getStatus());
        verify(listingRepository, times(1)).save(any(Listing.class));
    }

    /**
     * Tests that getListings returns all active listings when no category
     * filter is provided.
     */
    @Test
    void GetListings_ReturnsActiveListings_WhenNoCategoryFilterTest() {
        Listing listing1 = new Listing();
        listing1.setStatus(Listing.Status.ACTIVE);
        Listing listing2 = new Listing();
        listing2.setStatus(Listing.Status.ACTIVE);
        when(listingRepository.findByStatus(Listing.Status.ACTIVE))
                .thenReturn(List.of(listing1, listing2));

        java.util.List<Listing> result = listingService.getListings(null);
        assertEquals(2, result.size());
        verify(listingRepository, times(1)).findByStatus(Listing.Status.ACTIVE);
    }

    /**
     * Tests that getListings filters by category when a category filter is
     * provided.
     */
    @Test
    void GetListings_ReturnsFilteredListings_WhenCategoryFilterTest() {
        Listing listing1 = new Listing();
        listing1.setStatus(Listing.Status.ACTIVE);
        listing1.setCategory("Test Category");
        Listing listing2 = new Listing();
        listing2.setStatus(Listing.Status.ACTIVE);
        listing2.setCategory("Other Category");
        when(listingRepository.findByStatusAndCategory(
                Listing.Status.ACTIVE, "Test Category"))
                .thenReturn(List.of(listing1));

        java.util.List<Listing> result = listingService.getListings("Test Category");
        assertEquals(1, result.size());
        assertEquals("Test Category", result.get(0).getCategory());
        verify(listingRepository, times(1)).findByStatusAndCategory(
                Listing.Status.ACTIVE, "Test Category");
    }

    /**
     * Tests that getListingById returns the listed listing when it exists.
     */
    @Test
    void GetListingById_ReturnsListing_WhenListingExistsTest() {
        Listing listing = new Listing();
        listing.setId(1);
        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(listing));

        Listing result = listingService.getListingById(1);
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(listingRepository, times(1)).findById(1);
    }

    /**
     * Tests that getListingById throws ResourceNotFoundException when listing
     * does not exist.
     */
    @Test
    void GetListingById_ThrowsException_WhenListingNotFoundTest() {
        when(listingRepository.findById(1)).thenReturn(java.util.Optional.empty());

        assertThrows(t_12.backend.exception.ResourceNotFoundException.class, () -> {
            listingService.getListingById(1);
        });
        verify(listingRepository, times(1)).findById(1);
    }

    /**
     * Tests that updateListing saves and returns the updated listing when the
     * requester is the owner.
     */
    @Test
    void UpdateListing_ReturnsUpdatedListing_WhenOwnerTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);

        t_12.backend.api.listings.UpdateListingRequest request
                = new t_12.backend.api.listings.UpdateListingRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setPrice(new BigDecimal("10.00000000"));
        request.setCategory("Updated Category");
        request.setImageUrl("http://example.com/updated.jpg");
        request.setStatus(Listing.Status.ACTIVE);

        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(existing));
        when(listingRepository.save(any(Listing.class)))
                .thenReturn(existing);

        Listing result = listingService.updateListing(1, request, 9);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(listingRepository, times(1))
                .save(any(Listing.class));
    }

    /**
     * Tests that updateListing throws ForbiddenException when the requester is
     * not the owner.
     */
    @Test
    void UpdateListing_ThrowsException_WhenNotOwnerTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);

        t_12.backend.api.listings.UpdateListingRequest request
                = new t_12.backend.api.listings.UpdateListingRequest();

        when(listingRepository.findById(1))
                .thenReturn(java.util.Optional.of(existing));

        assertThrows(t_12.backend.exception.ForbiddenException.class,
                () -> listingService.updateListing(1, request, 99));
    }

    /**
     * Tests that deleteListing sets the listing status to REMOVED when the
     * requester is the owner.
     */
    @Test
    void DeleteListing_SetsStatusRemoved_WhenOwnerRequestsTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);
        existing.setStatus(Listing.Status.ACTIVE);

        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(existing));

        listingService.deleteListing(1, 9);

        assertEquals(Listing.Status.REMOVED, existing.getStatus());
        verify(listingRepository, times(1)).save(existing);
    }

    /**
     * Tests that deleteListing throws ForbiddenException when a non-owner
     * requests.
     */
    @Test
    void DeleteListing_ThrowsException_WhenNonOwnerRequestsTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);

        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(existing));

        assertThrows(
                t_12.backend.exception.ForbiddenException.class,
                () -> listingService.deleteListing(1, 99)
        );
    }

    /**
     * A buyer with sufficient balance should be able to purchase an active listing.
     */
    @Test
    void purchaseListing_success() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByUserId(SELLER_ID)).thenReturn(Optional.of(sellerWallet));
        when(transactionService.generateTransactionHash(any(), any(), any(), any(), anyInt(), any(LocalDateTime.class)))
                .thenReturn("mock-tx-hash");
 
        String txHash = listingService.purchaseListing(LISTING_ID, BUYER_ID);
 
        assertNotNull(txHash);
        assertEquals("mock-tx-hash", txHash);
        assertEquals(SUFFICIENT_BALANCE.subtract(PRICE), buyerWallet.getCoinBalance());
        assertEquals(PRICE, sellerWallet.getCoinBalance());
        assertEquals(Listing.Status.SOLD, activeListing.getStatus());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(walletRepository, times(1)).save(buyerWallet);
        verify(walletRepository, times(1)).save(sellerWallet);
        verify(listingRepository, times(1)).save(activeListing);
    }
 
    /**
     * Should throw ResourceNotFoundException when the listing does not exist.
     */
    @Test
    void purchaseListing_listingNotFound_throwsResourceNotFoundException() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.empty());
 
        assertThrows(t_12.backend.exception.ResourceNotFoundException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));
 
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
     * Should throw IllegalStateException when the buyer is also the seller.
     */
    @Test
    void purchaseListing_buyerIsSeller_throwsIllegalStateException() {
        activeListing.setSellerId(BUYER_ID);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
 
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));
 
        assertEquals("You cannot purchase your own listing", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }
 
    /**
     * Should throw IllegalStateException when the buyer does not have enough TimeCoin.
     */
    @Test
    void purchaseListing_insufficientBalance_throwsIllegalStateException() {
        buyerWallet.setCoinBalance(INSUFFICIENT_BALANCE);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));
 
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));
 
        assertEquals("Insufficient balance. Required: " + PRICE
                + ", Available: " + INSUFFICIENT_BALANCE, ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }
 
    /**
     * Should throw ResourceNotFoundException when the buyer has no wallet.
     */
    @Test
    void purchaseListing_buyerWalletNotFound_throwsResourceNotFoundException() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.empty());
 
        assertThrows(t_12.backend.exception.ResourceNotFoundException.class,
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
 
        assertThrows(t_12.backend.exception.ResourceNotFoundException.class,
                () -> listingService.purchaseListing(LISTING_ID, BUYER_ID));
 
        verify(transactionRepository, never()).save(any());
    }
 
    /**
     * Edge case — balance exactly equal to price should succeed.
     */
    @Test
    void purchaseListing_balanceExactlyEqualToPrice_succeeds() {
        buyerWallet.setCoinBalance(PRICE);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(activeListing));
        when(walletRepository.findByUserId(BUYER_ID)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByUserId(SELLER_ID)).thenReturn(Optional.of(sellerWallet));
        when(transactionService.generateTransactionHash(any(), any(), any(), any(), anyInt(), any(LocalDateTime.class)))
                .thenReturn("mock-tx-hash");
 
        String txHash = listingService.purchaseListing(LISTING_ID, BUYER_ID);
 
        assertNotNull(txHash);
        assertEquals(0, buyerWallet.getCoinBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, sellerWallet.getCoinBalance().compareTo(PRICE));
        assertEquals(Listing.Status.SOLD, activeListing.getStatus());
    }
}
