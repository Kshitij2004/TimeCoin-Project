package t_12.backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.exception.InvalidNonceException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.TransactionRepository;

/**
 * Manages the pending transaction pool (mempool).
 * Valid transactions are enqueued as PENDING, retrievable for block assembly,
 * and transitioned to CONFIRMED after block commit.
 */
@Service
public class MempoolService {

    private final TransactionRepository transactionRepository;
    private final TransactionValidationService transactionValidationService;

    public MempoolService(TransactionRepository transactionRepository,
                          TransactionValidationService transactionValidationService) {
        this.transactionRepository = transactionRepository;
        this.transactionValidationService = transactionValidationService;
    }

    /**
     * Validates and enqueues a transaction into the mempool as PENDING.
     * Rejects duplicate pending entries for the same canonical transfer tuple.
     */
    public Transaction enqueueValidatedTransaction(Transaction transaction) {
        transactionValidationService.validateBalance(
                transaction.getSenderAddress(),
                transaction.getAmount(),
                transaction.getFee()
        );
        transactionValidationService.validateNonce(
                transaction.getSenderAddress(),
                transaction.getNonce()
        );

        if (transaction.getTransactionHash() != null
                && transactionRepository.existsByTransactionHash(transaction.getTransactionHash())) {
            throw new DuplicateResourceException(
                    "Transaction with this hash already exists: " + transaction.getTransactionHash()
            );
        }

        if (isDuplicatePending(transaction)) {
            throw new DuplicateResourceException(
                    "Duplicate pending transaction for sender/receiver/amount/fee/nonce"
            );
        }

        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setBlockId(null);
        try {
            return transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            handleSaveConstraintViolation(transaction);
            throw ex;
        }
    }

    /**
     * Returns all transactions currently in PENDING state for block assembly.
     */
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatus(Transaction.Status.PENDING);
    }

    /**
     * Marks all provided transactions as CONFIRMED in a given block.
     */
    public void confirmTransactions(List<Transaction> transactions, Integer blockId) {
        for (Transaction tx : transactions) {
            tx.setStatus(Transaction.Status.CONFIRMED);
            tx.setBlockId(blockId);
            transactionRepository.save(tx);
        }
    }

    /**
     * Marks one transaction as CONFIRMED by transaction hash.
     */
    public Transaction confirmTransaction(String transactionHash, Integer blockId) {
        Transaction tx = transactionRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with hash: " + transactionHash
                ));

        tx.setStatus(Transaction.Status.CONFIRMED);
        tx.setBlockId(blockId);
        return transactionRepository.save(tx);
    }

    /**
     * Returns true if an equivalent PENDING transaction already exists. Handles
     * all three address shapes: buy (null sender), sell (null receiver), and
     * transfer (both addresses set).
     */
    private boolean isDuplicatePending(Transaction transaction) {
        String sender = transaction.getSenderAddress();
        String receiver = transaction.getReceiverAddress();

        // buy / coinbase: sender null, receiver set
        if (sender == null && receiver != null) {
            return transactionRepository
                    .existsBySenderAddressIsNullAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                            receiver,
                            transaction.getAmount(),
                            transaction.getFee(),
                            transaction.getNonce(),
                            Transaction.Status.PENDING
                    );
        }

        // sell: sender set, receiver null
        if (sender != null && receiver == null) {
            return transactionRepository
                    .existsBySenderAddressAndReceiverAddressIsNullAndAmountAndFeeAndNonceAndStatus(
                            sender,
                            transaction.getAmount(),
                            transaction.getFee(),
                            transaction.getNonce(),
                            Transaction.Status.PENDING
                    );
        }

        // transfer: both set
        return transactionRepository
                .existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                        sender,
                        receiver,
                        transaction.getAmount(),
                        transaction.getFee(),
                        transaction.getNonce(),
                        Transaction.Status.PENDING
                );
    }

    private void handleSaveConstraintViolation(Transaction transaction) {
        if (transaction.getSenderAddress() != null) {
            Integer latestNonce = transactionRepository.findMaxNonceBySenderAddressAndStatuses(
                    transaction.getSenderAddress(),
                    List.of(Transaction.Status.CONFIRMED, Transaction.Status.PENDING)
            );
            long expectedNonce = (latestNonce == null ? 0L : latestNonce.longValue()) + 1;
            Integer providedNonce = transaction.getNonce();
            if (providedNonce == null
                    || providedNonce < 0
                    || providedNonce.longValue() != expectedNonce) {
                throw new InvalidNonceException(expectedNonce, providedNonce);
            }
        }

        if (transaction.getTransactionHash() != null
                && transactionRepository.existsByTransactionHash(transaction.getTransactionHash())) {
            throw new DuplicateResourceException(
                    "Transaction with this hash already exists: " + transaction.getTransactionHash()
            );
        }

        throw new DuplicateResourceException("Transaction violates database uniqueness constraints.");
    }
}