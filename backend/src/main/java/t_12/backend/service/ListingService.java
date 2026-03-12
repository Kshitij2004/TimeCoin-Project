package t_12.backend.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.entity.Listing;
import t_12.backend.entity.Transaction;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.ListingRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Service handling the purchase of a marketplace listing with TimeCoin.
 *
 * Implements the full purchase flow:
 *   1. Validate the listing exists and is still available
 *   2. Prevents a seller from buying their own listing
 *   3. Validates the buyer has sufficient balance
 *   4. Subtracts from the buyer's wallet and credits the seller's wallet
 *   5. Creates a PENDING transaction (enters the mempool)
 *   6. Marks the listing as SOLD
 *
 * The entire operation is wrapped in @Transactional — if any step fails,
 * all database changes are rolled back automatically.
 */
@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public ListingService(
            ListingRepository listingRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {
        this.listingRepository = listingRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Executes the purchase of a marketplace listing.
     *
     * All database operations occur within a single transaction. Any failure
     * after wallet debiting (e.g. saving the transaction record) will trigger
     * a full rollback, preventing partial state.
     *
     * @param listingId   the ID of the listing to purchase
     * @param buyerUserId the ID of the user making the purchase
     * @return the SHA-256 transaction hash for tracking on the chain
     * @throws ResourceNotFoundException if the listing, buyer wallet, or seller wallet is not found
     * @throws IllegalStateException     if the listing is not ACTIVE, the buyer is the seller,
     *                                   or the buyer has insufficient balance
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
        if (buyerWallet.getCoinBalance().compareTo(price) < 0) {
            throw new IllegalStateException(
                    "Insufficient balance. Required: " + price
                            + ", Available: " + buyerWallet.getCoinBalance());
        }

        // 6. Load the seller's wallet — throws 404 if not found
        Wallet sellerWallet = walletRepository.findByUserId(listing.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller wallet not found for sellerId: " + listing.getSellerId()));

        // 7. Transfer funds: debit buyer, credit seller
        buyerWallet.setCoinBalance(buyerWallet.getCoinBalance().subtract(price));
        sellerWallet.setCoinBalance(sellerWallet.getCoinBalance().add(price));
        walletRepository.save(buyerWallet);
        walletRepository.save(sellerWallet);

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
        tx.setTransactionHash(generateHash(buyerWallet.getWalletAddress(),
                sellerWallet.getWalletAddress(), price, now));
        transactionRepository.save(tx);

        // 9. Mark the listing as SOLD so it can no longer be purchased
        listing.setStatus(Listing.Status.SOLD);
        listingRepository.save(listing);

        // Return the transaction hash so the buyer can track it on the chain
        return tx.getTransactionHash();
    }

    /**
     * Generates a SHA-256 hash to uniquely identify a transaction.
     *
     * The hash is derived from the sender address, receiver address, amount,
     * timestamp, and a nanosecond timestamp to ensure uniqueness even for
     * identical transfers made at the same time.
     *
     * @param sender    wallet address of the sender
     * @param receiver  wallet address of the receiver
     * @param amount    amount of TimeCoin transferred
     * @param timestamp time the transaction was created
     * @return a hex-encoded SHA-256 hash string
     */
    private String generateHash(String sender, String receiver, BigDecimal amount, LocalDateTime timestamp) {
        try {
            String raw = sender + receiver + amount.toPlainString() + timestamp.toString()
                    + System.nanoTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate transaction hash", e);
        }
    }
}

