package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.dao.DataIntegrityViolationException;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.exception.InvalidNonceException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class MempoolServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidationService transactionValidationService;

    @InjectMocks
    private MempoolService mempoolService;

    private Transaction tx;

    @BeforeEach
    void setUp() {
        tx = new Transaction();
        tx.setId(1);
        tx.setSenderAddress("sender_1");
        tx.setReceiverAddress("receiver_1");
        tx.setAmount(new BigDecimal("10.00000000"));
        tx.setFee(new BigDecimal("0.01000000"));
        tx.setNonce(1);
        tx.setTimestamp(LocalDateTime.of(2026, 3, 21, 10, 30, 0));
        tx.setTransactionHash("hash_1");
    }

    @Test
    void enqueueValidatedTransaction_validTransaction_entersPendingPool() {
        when(transactionRepository.existsByTransactionHash("hash_1")).thenReturn(false);
        when(transactionRepository.existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                tx.getSenderAddress(),
                tx.getReceiverAddress(),
                tx.getAmount(),
                tx.getFee(),
                tx.getNonce(),
                Transaction.Status.PENDING
        )).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction saved = mempoolService.enqueueValidatedTransaction(tx);

        assertEquals(Transaction.Status.PENDING, saved.getStatus());
        assertNull(saved.getBlockId());
        verify(transactionValidationService).validateBalance(tx.getSenderAddress(), tx.getAmount(), tx.getFee());
        verify(transactionValidationService).validateNonce(tx.getSenderAddress(), tx.getNonce());
        verify(transactionRepository).save(tx);
    }

    @Test
    void enqueueValidatedTransaction_duplicatePending_throwsDuplicateResourceException() {
        when(transactionRepository.existsByTransactionHash("hash_1")).thenReturn(false);
        when(transactionRepository.existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                tx.getSenderAddress(),
                tx.getReceiverAddress(),
                tx.getAmount(),
                tx.getFee(),
                tx.getNonce(),
                Transaction.Status.PENDING
        )).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> mempoolService.enqueueValidatedTransaction(tx));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void enqueueValidatedTransaction_duplicateHash_throwsDuplicateResourceException() {
        when(transactionRepository.existsByTransactionHash("hash_1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> mempoolService.enqueueValidatedTransaction(tx));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void enqueueValidatedTransaction_dbNonceConflict_throwsInvalidNonceException() {
        when(transactionRepository.existsByTransactionHash("hash_1")).thenReturn(false);
        when(transactionRepository.existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                tx.getSenderAddress(),
                tx.getReceiverAddress(),
                tx.getAmount(),
                tx.getFee(),
                tx.getNonce(),
                Transaction.Status.PENDING
        )).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate sender nonce"));
        when(transactionRepository.findMaxNonceBySenderAddressAndStatuses(
                tx.getSenderAddress(),
                List.of(Transaction.Status.CONFIRMED, Transaction.Status.PENDING)
        )).thenReturn(1);

        InvalidNonceException ex = assertThrows(
                InvalidNonceException.class,
                () -> mempoolService.enqueueValidatedTransaction(tx)
        );

        assertTrue(ex.getMessage().contains("expected 2"));
        assertTrue(ex.getMessage().contains("received 1"));
    }

    @Test
    void enqueueValidatedTransaction_dbConstraintButNonceStillValid_mapsToDuplicateHashConflict() {
        when(transactionRepository.existsByTransactionHash("hash_1")).thenReturn(false, true);
        when(transactionRepository.existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                tx.getSenderAddress(),
                tx.getReceiverAddress(),
                tx.getAmount(),
                tx.getFee(),
                tx.getNonce(),
                Transaction.Status.PENDING
        )).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate transaction hash"));
        when(transactionRepository.findMaxNonceBySenderAddressAndStatuses(
                tx.getSenderAddress(),
                List.of(Transaction.Status.CONFIRMED, Transaction.Status.PENDING)
        )).thenReturn(0);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> mempoolService.enqueueValidatedTransaction(tx)
        );

        assertTrue(ex.getMessage().contains("Transaction with this hash already exists"));
    }

    @Test
    void getPendingTransactions_returnsAllPending() {
        Transaction tx1 = new Transaction();
        tx1.setStatus(Transaction.Status.PENDING);
        Transaction tx2 = new Transaction();
        tx2.setStatus(Transaction.Status.PENDING);
        when(transactionRepository.findByStatus(Transaction.Status.PENDING)).thenReturn(List.of(tx1, tx2));

        List<Transaction> pending = mempoolService.getPendingTransactions();

        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(t -> t.getStatus() == Transaction.Status.PENDING));
    }

    @Test
    void confirmTransactions_setsConfirmedAndBlockId() {
        tx.setStatus(Transaction.Status.PENDING);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mempoolService.confirmTransactions(List.of(tx), 7);

        assertEquals(Transaction.Status.CONFIRMED, tx.getStatus());
        assertEquals(7, tx.getBlockId());
        verify(transactionRepository).save(tx);
    }

    @Test
    void confirmTransaction_byHash_setsConfirmed() {
        tx.setStatus(Transaction.Status.PENDING);
        when(transactionRepository.findByTransactionHash("hash_1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction confirmed = mempoolService.confirmTransaction("hash_1", 8);

        assertEquals(Transaction.Status.CONFIRMED, confirmed.getStatus());
        assertEquals(8, confirmed.getBlockId());
    }

    @Test
    void confirmTransaction_missingHash_throwsResourceNotFoundException() {
        when(transactionRepository.findByTransactionHash("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> mempoolService.confirmTransaction("missing", 9));
    }
}
