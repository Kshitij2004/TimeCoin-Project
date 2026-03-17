package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.TransactionRepository;

/**
 * Unit tests for TransactionService. Uses Mockito to mock the repository
 * layer so tests run fast without a database. Covers hash generation,
 * blockchain transfers, marketplace transactions, lookups, status
 * transitions, and error cases.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private String senderAddress;
    private String receiverAddress;
    private BigDecimal amount;
    private BigDecimal fee;
    private Integer nonce;

    @BeforeEach
    void setUp() {
        senderAddress = "TC_addr_sender_aaa111";
        receiverAddress = "TC_addr_receiver_bbb222";
        amount = new BigDecimal("50.00000000");
        fee = new BigDecimal("0.01000000");
        nonce = 1;
    }

    // Hash Generation Tests

    @Test
    void generateTransactionHash_sameInputs_producesSameHash() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        String hash1 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp);
        String hash2 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp);

        assertEquals(hash1, hash2, "Same inputs must produce the same hash");
    }

    @Test
    void generateTransactionHash_differentAmount_producesDifferentHash() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 10, 12, 0, 0);
        BigDecimal differentAmount = new BigDecimal("100.00000000");

        String hash1 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp);
        String hash2 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, differentAmount, fee, nonce, timestamp);

        assertNotEquals(hash1, hash2, "Different amounts must produce different hashes");
    }

    @Test
    void generateTransactionHash_differentSender_producesDifferentHash() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        String hash1 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp);
        String hash2 = transactionService.generateTransactionHash(
                "TC_addr_different_xxx999", receiverAddress, amount, fee, nonce, timestamp);

        assertNotEquals(hash1, hash2, "Different senders must produce different hashes");
    }

    @Test
    void generateTransactionHash_differentNonce_producesDifferentHash() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        String hash1 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp);
        String hash2 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, 2, timestamp);

        assertNotEquals(hash1, hash2, "Different nonces must produce different hashes");
    }

    @Test
    void generateTransactionHash_differentTimestamp_producesDifferentHash() {
        LocalDateTime timestamp1 = LocalDateTime.of(2026, 3, 10, 12, 0, 0);
        LocalDateTime timestamp2 = LocalDateTime.of(2026, 3, 10, 12, 0, 1);

        String hash1 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp1);
        String hash2 = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp2);

        assertNotEquals(hash1, hash2, "Different timestamps must produce different hashes");
    }

    @Test
    void generateTransactionHash_nullSender_handledGracefully() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        String hash = transactionService.generateTransactionHash(
                null, receiverAddress, amount, fee, nonce, timestamp);

        assertNotNull(hash, "Hash should be generated even with null sender");
        assertEquals(64, hash.length(), "SHA-256 hex hash should be 64 characters");
    }

    @Test
    void generateTransactionHash_returnsValidSHA256Format() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 10, 12, 0, 0);

        String hash = transactionService.generateTransactionHash(
                senderAddress, receiverAddress, amount, fee, nonce, timestamp);

        assertEquals(64, hash.length(), "SHA-256 hex hash should be 64 characters");
        assertTrue(hash.matches("[0-9a-f]+"), "Hash should only contain hex characters");
    }

    // Create Blockchain Transfer Tests

    @Test
    void createTransaction_validInputs_returnsPendingWithNullMarketplaceFields() {
        when(transactionRepository.existsByTransactionHash(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(1);
            return tx;
        });

        Transaction result = transactionService.createTransaction(
                senderAddress, receiverAddress, amount, fee, nonce);

        assertNotNull(result);
        assertEquals(Transaction.Status.PENDING, result.getStatus());
        assertEquals(senderAddress, result.getSenderAddress());
        assertEquals(receiverAddress, result.getReceiverAddress());
        assertEquals(amount, result.getAmount());
        assertEquals(fee, result.getFee());
        assertEquals(nonce, result.getNonce());
        assertNotNull(result.getTransactionHash());
        assertNull(result.getBlockId());
        // marketplace fields should be null for pure blockchain transfers
        assertNull(result.getUserId());
        assertNull(result.getSymbol());
        assertNull(result.getTransactionType());
        assertNull(result.getPriceAtTime());
        assertNull(result.getTotalUsd());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_duplicateHash_throwsDuplicateResourceException() {
        when(transactionRepository.existsByTransactionHash(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                transactionService.createTransaction(
                        senderAddress, receiverAddress, amount, fee, nonce)
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // Create Marketplace Transaction Tests

    @Test
    void createMarketplaceTransaction_validInputs_setsMarketplaceFields() {
        when(transactionRepository.existsByTransactionHash(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(2);
            return tx;
        });

        Transaction result = transactionService.createMarketplaceTransaction(
                senderAddress, receiverAddress, amount, fee, nonce,
                1, "TC", Transaction.TransactionType.BUY,
                new BigDecimal("10.00"), new BigDecimal("500.00"));

        assertNotNull(result);
        assertEquals(Transaction.Status.PENDING, result.getStatus());
        assertEquals(1, result.getUserId());
        assertEquals("TC", result.getSymbol());
        assertEquals(Transaction.TransactionType.BUY, result.getTransactionType());
        assertEquals(new BigDecimal("10.00"), result.getPriceAtTime());
        assertEquals(new BigDecimal("500.00"), result.getTotalUsd());
        assertNotNull(result.getTransactionHash());
        assertNull(result.getBlockId());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createMarketplaceTransaction_duplicateHash_throwsDuplicateResourceException() {
        when(transactionRepository.existsByTransactionHash(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                transactionService.createMarketplaceTransaction(
                        senderAddress, receiverAddress, amount, fee, nonce,
                        1, "TC", Transaction.TransactionType.BUY,
                        new BigDecimal("10.00"), new BigDecimal("500.00"))
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // Find Tests

    @Test
    void findByHash_existingHash_returnsTransaction() {
        Transaction mockTx = new Transaction();
        mockTx.setId(1);
        mockTx.setTransactionHash("abc123");
        mockTx.setStatus(Transaction.Status.PENDING);

        when(transactionRepository.findByTransactionHash("abc123"))
                .thenReturn(Optional.of(mockTx));

        Transaction result = transactionService.findByHash("abc123");

        assertNotNull(result);
        assertEquals("abc123", result.getTransactionHash());
    }

    @Test
    void findByHash_nonExistentHash_throwsResourceNotFoundException() {
        when(transactionRepository.findByTransactionHash("nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.findByHash("nonexistent")
        );
    }

    @Test
    void findById_existingId_returnsTransaction() {
        Transaction mockTx = new Transaction();
        mockTx.setId(1);
        mockTx.setTransactionHash("abc123");

        when(transactionRepository.findById(1)).thenReturn(Optional.of(mockTx));

        Transaction result = transactionService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void findById_nonExistentId_throwsResourceNotFoundException() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.findById(999)
        );
    }

    @Test
    void findByWalletAddress_returnsTxsAsSenderAndReceiver() {
        Transaction tx1 = new Transaction();
        tx1.setSenderAddress(senderAddress);
        tx1.setReceiverAddress("other");

        Transaction tx2 = new Transaction();
        tx2.setSenderAddress("other");
        tx2.setReceiverAddress(senderAddress);

        when(transactionRepository.findBySenderAddressOrReceiverAddress(senderAddress, senderAddress))
                .thenReturn(Arrays.asList(tx1, tx2));

        List<Transaction> results = transactionService.findByWalletAddress(senderAddress);

        assertEquals(2, results.size());
    }

    @Test
    void findBySender_returnsOnlySentTransactions() {
        Transaction tx1 = new Transaction();
        tx1.setSenderAddress(senderAddress);

        when(transactionRepository.findBySenderAddress(senderAddress))
                .thenReturn(List.of(tx1));

        List<Transaction> results = transactionService.findBySender(senderAddress);

        assertEquals(1, results.size());
        assertEquals(senderAddress, results.get(0).getSenderAddress());
    }

    @Test
    void findByReceiver_returnsOnlyReceivedTransactions() {
        Transaction tx1 = new Transaction();
        tx1.setReceiverAddress(receiverAddress);

        when(transactionRepository.findByReceiverAddress(receiverAddress))
                .thenReturn(List.of(tx1));

        List<Transaction> results = transactionService.findByReceiver(receiverAddress);

        assertEquals(1, results.size());
        assertEquals(receiverAddress, results.get(0).getReceiverAddress());
    }

    @Test
    void findByStatus_returnsPendingTransactions() {
        Transaction tx1 = new Transaction();
        tx1.setStatus(Transaction.Status.PENDING);

        Transaction tx2 = new Transaction();
        tx2.setStatus(Transaction.Status.PENDING);

        when(transactionRepository.findByStatus(Transaction.Status.PENDING))
                .thenReturn(Arrays.asList(tx1, tx2));

        List<Transaction> results = transactionService.findByStatus(Transaction.Status.PENDING);

        assertEquals(2, results.size());
    }

    @Test
    void findByBlockId_returnsTransactionsInBlock() {
        Transaction tx1 = new Transaction();
        tx1.setBlockId(5);

        when(transactionRepository.findByBlockId(5)).thenReturn(List.of(tx1));

        List<Transaction> results = transactionService.findByBlockId(5);

        assertEquals(1, results.size());
        assertEquals(5, results.get(0).getBlockId());
    }

    // Status Update Tests

    @Test
    void updateStatus_pendingToConfirmed_updatesSuccessfully() {
        Transaction mockTx = new Transaction();
        mockTx.setId(1);
        mockTx.setTransactionHash("abc123");
        mockTx.setStatus(Transaction.Status.PENDING);

        when(transactionRepository.findByTransactionHash("abc123"))
                .thenReturn(Optional.of(mockTx));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.updateStatus("abc123", Transaction.Status.CONFIRMED);

        assertEquals(Transaction.Status.CONFIRMED, result.getStatus());
        verify(transactionRepository).save(mockTx);
    }

    @Test
    void updateStatus_pendingToRejected_updatesSuccessfully() {
        Transaction mockTx = new Transaction();
        mockTx.setId(1);
        mockTx.setTransactionHash("abc123");
        mockTx.setStatus(Transaction.Status.PENDING);

        when(transactionRepository.findByTransactionHash("abc123"))
                .thenReturn(Optional.of(mockTx));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.updateStatus("abc123", Transaction.Status.REJECTED);

        assertEquals(Transaction.Status.REJECTED, result.getStatus());
    }

    @Test
    void updateStatus_nonExistentHash_throwsResourceNotFoundException() {
        when(transactionRepository.findByTransactionHash("nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.updateStatus("nonexistent", Transaction.Status.CONFIRMED)
        );
    }

    // Link to Block Tests

    @Test
    void linkToBlock_setsBlockIdAndConfirmsTransaction() {
        Transaction mockTx = new Transaction();
        mockTx.setId(1);
        mockTx.setTransactionHash("abc123");
        mockTx.setStatus(Transaction.Status.PENDING);

        when(transactionRepository.findByTransactionHash("abc123"))
                .thenReturn(Optional.of(mockTx));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.linkToBlock("abc123", 5);

        assertEquals(5, result.getBlockId());
        assertEquals(Transaction.Status.CONFIRMED, result.getStatus());
    }

    @Test
    void linkToBlock_nonExistentHash_throwsResourceNotFoundException() {
        when(transactionRepository.findByTransactionHash("nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transactionService.linkToBlock("nonexistent", 5)
        );
    }
}