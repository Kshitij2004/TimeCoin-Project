package t_12.backend.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.TransactionRepository;

/**
 * Service class for handling transaction-related business logic. Manages
 * transaction creation, lookup, status transitions, and SHA-256 hash generation
 * over canonical transaction fields. Supports both pure blockchain transfers
 * and marketplace purchase history records.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MempoolService mempoolService;

    public TransactionService(TransactionRepository transactionRepository, MempoolService mempoolService) {
        this.transactionRepository = transactionRepository;
        this.mempoolService = mempoolService;
    }

    /**
     * Creates a new blockchain transfer transaction with status PENDING.
     * Generates a deterministic SHA-256 hash over the canonical fields. Rejects
     * duplicate transaction hashes.
     *
     * @param senderAddress the wallet address of the sender (nullable for
     * minting)
     * @param receiverAddress the wallet address of the receiver
     * @param amount the amount of TimeCoin to transfer
     * @param fee the transaction fee
     * @param nonce the sender's transaction sequence number
     * @return the saved Transaction entity with generated hash and PENDING
     * status
     * @throws DuplicateResourceException if a transaction with the same hash
     * already exists
     */
    public Transaction createTransaction(String senderAddress,
            String receiverAddress, BigDecimal amount,
            BigDecimal fee, Integer nonce) {
        LocalDateTime timestamp = LocalDateTime.now();
        String hash = generateTransactionHash(senderAddress, receiverAddress, amount, fee, nonce, timestamp);

        Transaction transaction = new Transaction();
        transaction.setSenderAddress(senderAddress);
        transaction.setReceiverAddress(receiverAddress);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setNonce(nonce);
        transaction.setTimestamp(timestamp);
        transaction.setTransactionHash(hash);
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setBlockId(null);
        // marketplace fields left null for pure blockchain transfers
        transaction.setUserId(null);
        transaction.setSymbol(null);
        transaction.setTransactionType(null);
        transaction.setPriceAtTime(null);
        transaction.setTotalUsd(null);

        return mempoolService.enqueueValidatedTransaction(transaction);
    }

    /**
     * Creates a marketplace purchase transaction with status PENDING. Includes
     * marketplace-specific fields (userId, symbol, transactionType,
     * priceAtTime, totalUsd) alongside the blockchain fields.
     *
     * @param senderAddress the buyer's wallet address
     * @param receiverAddress the seller's wallet address
     * @param amount the amount of TimeCoin
     * @param fee the transaction fee
     * @param nonce the sender's sequence number
     * @param userId the user ID for purchase history
     * @param symbol the coin symbol (e.g. "TC")
     * @param transactionType the type (BUY, SELL, etc.)
     * @param priceAtTime the coin price at time of transaction
     * @param totalUsd the total USD equivalent
     * @return the saved Transaction entity
     * @throws DuplicateResourceException if a transaction with the same hash
     * already exists
     */
    public Transaction createMarketplaceTransaction(String senderAddress, String receiverAddress,
            BigDecimal amount, BigDecimal fee, Integer nonce,
            Integer userId, String symbol,
            Transaction.TransactionType transactionType,
            BigDecimal priceAtTime, BigDecimal totalUsd) {
        LocalDateTime timestamp = LocalDateTime.now();
        String hash = generateTransactionHash(senderAddress, receiverAddress, amount, fee, nonce, timestamp);

        Transaction transaction = new Transaction();
        transaction.setSenderAddress(senderAddress);
        transaction.setReceiverAddress(receiverAddress);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setNonce(nonce);
        transaction.setTimestamp(timestamp);
        transaction.setTransactionHash(hash);
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setBlockId(null);
        transaction.setUserId(userId);
        transaction.setSymbol(symbol);
        transaction.setTransactionType(transactionType);
        transaction.setPriceAtTime(priceAtTime);
        transaction.setTotalUsd(totalUsd);

        return mempoolService.enqueueValidatedTransaction(transaction);
    }

    /**
     * Retrieves a transaction by its hash.
     *
     * @param transactionHash the SHA-256 hash of the transaction
     * @return the Transaction entity
     * @throws ResourceNotFoundException if no transaction is found with the
     * given hash
     */
    public Transaction findByHash(String transactionHash) {
        return transactionRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found with hash: " + transactionHash
        ));
    }

    /**
     * Retrieves a transaction by its ID.
     *
     * @param id the transaction ID
     * @return the Transaction entity
     * @throws ResourceNotFoundException if no transaction is found with the
     * given ID
     */
    public Transaction findById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found with id: " + id
        ));
    }

    /**
     * Retrieves all transactions where the given address is either sender or
     * receiver. Useful for displaying a wallet's full transaction history.
     *
     * @param walletAddress the wallet address to search for
     * @return list of transactions involving the address
     */
    public List<Transaction> findByWalletAddress(String walletAddress) {
        return transactionRepository.findBySenderAddressOrReceiverAddress(walletAddress, walletAddress);
    }

    /**
     * Retrieves all transactions sent from a specific address.
     *
     * @param senderAddress the sender wallet address
     * @return list of transactions from the address
     */
    public List<Transaction> findBySender(String senderAddress) {
        return transactionRepository.findBySenderAddress(senderAddress);
    }

    /**
     * Retrieves all transactions received by a specific address.
     *
     * @param receiverAddress the receiver wallet address
     * @return list of transactions to the address
     */
    public List<Transaction> findByReceiver(String receiverAddress) {
        return transactionRepository.findByReceiverAddress(receiverAddress);
    }

    /**
     * Retrieves all transactions with a given status.
     *
     * @param status the status to filter by (PENDING, CONFIRMED, or REJECTED)
     * @return list of transactions with the given status
     */
    public List<Transaction> findByStatus(Transaction.Status status) {
        return transactionRepository.findByStatus(status);
    }

    /**
     * Retrieves all transactions linked to a specific block.
     *
     * @param blockId the block ID
     * @return list of transactions in the block
     */
    public List<Transaction> findByBlockId(Integer blockId) {
        return transactionRepository.findByBlockId(blockId);
    }

    /**
     * Retrieves all transactions in the system.
     *
     * @return list of all transactions
     */
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    /**
     * Updates the status of a transaction. Used during block assembly to move
     * transactions from PENDING to CONFIRMED, or to REJECTED if validation
     * fails during block commit.
     *
     * @param transactionHash the hash of the transaction to update
     * @param newStatus the new status
     * @return the updated Transaction entity
     * @throws ResourceNotFoundException if no transaction is found with the
     * given hash
     */
    public Transaction updateStatus(String transactionHash, Transaction.Status newStatus) {
        Transaction transaction = findByHash(transactionHash);
        transaction.setStatus(newStatus);
        return transactionRepository.save(transaction);
    }

    /**
     * Links a transaction to a block by setting its blockId and status to
     * CONFIRMED. Called during block commit to associate confirmed transactions
     * with their block.
     *
     * @param transactionHash the hash of the transaction
     * @param blockId the ID of the block that includes this transaction
     * @return the updated Transaction entity
     * @throws ResourceNotFoundException if no transaction is found with the
     * given hash
     */
    public Transaction linkToBlock(String transactionHash, Integer blockId) {
        Transaction transaction = findByHash(transactionHash);
        transaction.setBlockId(blockId);
        transaction.setStatus(Transaction.Status.CONFIRMED);
        return transactionRepository.save(transaction);
    }

    /**
     * Generates a deterministic SHA-256 hash over the canonical transaction
     * fields. The canonical format is a pipe-delimited string:
     * "sender|receiver|amount|fee|nonce|timestamp"
     *
     * The same inputs always produce the same hash. Changing any single field
     * produces a completely different hash. This is how we detect tampering and
     * prevent duplicate transactions.
     *
     * @param senderAddress the sender wallet address
     * @param receiverAddress the receiver wallet address
     * @param amount the transfer amount
     * @param fee the transaction fee
     * @param nonce the sequence number
     * @param timestamp the transaction timestamp
     * @return the hex-encoded SHA-256 hash string
     */
    public String generateTransactionHash(String senderAddress, String receiverAddress,
            BigDecimal amount, BigDecimal fee,
            Integer nonce, LocalDateTime timestamp) {

        // pipe delimiter prevents field bleed. "AB|CD" hashes differently from "ABC|D"
        String canonical = (senderAddress != null ? senderAddress : "null")
                + "|" + receiverAddress
                + "|" + amount.toPlainString()
                + "|" + fee.toPlainString()
                + "|" + nonce
                + "|" + timestamp.toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed in every JVM. this should never execute.
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts a byte array to a lowercase hex string.
     *
     * @param bytes the byte array to convert
     * @return the hex-encoded string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
