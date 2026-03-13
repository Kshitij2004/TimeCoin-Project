package t_12.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Transaction;

/**
 * Data access methods for blockchain transactions.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    // Finds a transaction by its unique hash.
    Optional<Transaction> findByTransactionHash(String transactionHash);
    // Returns transactions sent from a wallet.
    List<Transaction> findBySenderAddress(String senderAddress);
    // Returns transactions received by a wallet.
    List<Transaction> findByReceiverAddress(String receiverAddress);
    // Returns transactions matching the requested processing state.
    List<Transaction> findByStatus(Transaction.Status status);
    // Returns transactions assigned to a specific block.
    List<Transaction> findByBlockId(Integer blockId);
}
