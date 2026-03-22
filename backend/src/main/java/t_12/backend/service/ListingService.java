package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.api.listings.CreateListingRequest;
import t_12.backend.api.listings.UpdateListingRequest;
import t_12.backend.entity.Listing;
import t_12.backend.entity.Transaction;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.ForbiddenException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.ListingRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Service class for handling marketplace listing business logic. Manages
 * listing creation, retrieval, updates, and removal.
 */
@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final BalanceService balanceService;

    public ListingService(ListingRepository listingRepository, WalletRepository walletRepository,
            TransactionRepository transactionRepository, TransactionService transactionService, BalanceService balanceService) {
        this.listingRepository = listingRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.balanceService = balanceService;
    }

    /**
     * Creates a new marketplace listing for the authenticated seller.
     *
     * @param request the listing details from the client
     * @param sellerId the ID of the authenticated user creating the listing
     * @return the saved Listing entity
     */
    public Listing createListing(CreateListingRequest request, Integer sellerId) {
        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategory(request.getCategory());
        listing.setImageUrl(request.getImageUrl());

        // New listings always start as ACTIVE.
        // Status can only change via updateListing or deleteListing.
        listing.setStatus(Listing.Status.ACTIVE);
        listing.setCreatedAt(LocalDateTime.now());

        return listingRepository.save(listing);
    }

    /**
     * Retrieves active listings, optionally filtered by category.
     *
     * @param category optional category filter; if null, returns all active
     * listings
     * @return list of matching active listings
     */
    public List<Listing> getListings(String category) {
        if (category != null && !category.isBlank()) {
            return listingRepository.findByStatusAndCategory(
                    Listing.Status.ACTIVE, category);
        }
        return listingRepository.findByStatus(Listing.Status.ACTIVE);
    }

    /**
     * Retrieves a single listing by its ID.
     *
     * @param id the listing ID
     * @return the Listing entity
     * @throws ResourceNotFoundException if no listing with that ID exists
     */
    public Listing getListingById(Integer id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Listing not found: " + id));
    }

    /**
     * Updates an existing listing. Only the seller who created the listing may
     * update it.
     *
     * @param id the listing ID to update
     * @param request the updated fields from the client
     * @param sellerId the ID of the authenticated user making the request
     * @return the updated Listing entity
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws ForbiddenException if the requesting user is not the seller
     */
    public Listing updateListing(Integer id, UpdateListingRequest request,
            Integer sellerId) {
        Listing listing = getListingById(id);

        // Ownership check, only the seller can modify their own listing.
        // We compare the authenticated user's ID against the stored sellerId.
        if (!listing.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("Forbidden: you do not own this listing");
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategory(request.getCategory());
        listing.setImageUrl(request.getImageUrl());
        listing.setStatus(request.getStatus());

        return listingRepository.save(listing);
    }

    /**
     * Removes a listing by setting its status to REMOVED. Only the seller who
     * created the listing may remove it. The row is kept for history.
     *
     * @param id the listing ID to remove
     * @param sellerId the ID of the authenticated user making the request
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws ForbiddenException if the requesting user is not the seller
     */
    public void deleteListing(Integer id, Integer sellerId) {
        Listing listing = getListingById(id);

        // Ownership check. Same pattern as updateListing.
        if (!listing.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("Forbidden: you do not own this listing");
        }

        // Soft delete. Set status to REMOVED rather than deleting the row.
        // This preserves listing history and matches the status transitions
        // defined in the issue.
        listing.setStatus(Listing.Status.REMOVED);
        listingRepository.save(listing);
    }

    /**
     * Executes the purchase of a marketplace listing.
     *
     * All database operations occur within a single transaction. Any failure
     * after wallet debiting (e.g. saving the transaction record) will trigger a
     * full rollback, preventing partial state.
     *
     * @param listingId the ID of the listing to purchase
     * @param buyerUserId the ID of the user making the purchase
     * @return the SHA-256 transaction hash for tracking on the chain
     * @throws ResourceNotFoundException if the listing, buyer wallet, or seller
     * wallet is not found
     * @throws IllegalStateException if the listing is not ACTIVE, the buyer is
     * the seller, or the buyer has insufficient balance
     */
    @Transactional
    public String purchaseListing(Integer listingId, Integer buyerUserId) {

        // 1. Load the listing — throws 404 if not found
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Listing not found: " + listingId));

        // 2. Reject if the listing is no longer available (already SOLD or REMOVED)
        if (listing.getStatus() != Listing.Status.ACTIVE) {
            throw new IllegalStateException(
                    "Listing is no longer available (status: " + listing.getStatus() + ")");
        }

        // 3. Reject if the buyer is the seller — cannot purchase your own listing
        if (listing.getSellerId().equals(buyerUserId)) {
            throw new IllegalStateException("You cannot purchase your own listing");
        }

        // 4. Load the buyer's wallet — throws 404 if not found
        Wallet buyerWallet = walletRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Buyer wallet not found for userId: " + buyerUserId));

        // 5. Validate the buyer has enough TimeCoin to cover the listing price
        BigDecimal price = listing.getPrice();
        transactionValidationService.validateBalance(buyerWallet.getWalletAddress(), price, BigDecimal.ZERO);

        // 6. Load the seller's wallet — throws 404 if not found
        Wallet sellerWallet = walletRepository.findByUserId(listing.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                "Seller wallet not found for sellerId: " + listing.getSellerId()));

        // 8. Record the transaction as PENDING in the database (acts as mempool entry).
        //    The block assembler will later pick this up and include it in a block.
        LocalDateTime now = LocalDateTime.now();
        Transaction tx = new Transaction();
        tx.setSenderAddress(buyerWallet.getWalletAddress());
        tx.setReceiverAddress(sellerWallet.getWalletAddress());
        tx.setAmount(price);
        tx.setFee(BigDecimal.ZERO);
        tx.setNonce(0); // TODO: implement per-sender nonce tracking in validation service
        tx.setTimestamp(now);
        tx.setStatus(Transaction.Status.PENDING);
        tx.setTransactionHash(transactionService.generateTransactionHash(buyerWallet.getWalletAddress(),
                sellerWallet.getWalletAddress(), price, BigDecimal.ZERO, 0, now));
        transactionRepository.save(tx);

        // 9. Mark the listing as SOLD so it can no longer be purchased
        listing.setStatus(Listing.Status.SOLD);
        listingRepository.save(listing);

        // Return the transaction hash so the buyer can track it on the chain
        return tx.getTransactionHash();
    }
}
