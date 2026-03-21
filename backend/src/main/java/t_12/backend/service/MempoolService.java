package t_12.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Transaction;
import t_12.backend.exception.DuplicateResourceException;
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
        return transactionRepository.save(transaction);
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

    private boolean isDuplicatePending(Transaction transaction) {
        if (transaction.getSenderAddress() == null) {
            return transactionRepository
                    .existsBySenderAddressIsNullAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                            transaction.getReceiverAddress(),
                            transaction.getAmount(),
                            transaction.getFee(),
                            transaction.getNonce(),
                            Transaction.Status.PENDING
                    );
        }

        return transactionRepository
                .existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
                        transaction.getSenderAddress(),
                        transaction.getReceiverAddress(),
                        transaction.getAmount(),
                        transaction.getFee(),
                        transaction.getNonce(),
                        Transaction.Status.PENDING
                );
    }
}
