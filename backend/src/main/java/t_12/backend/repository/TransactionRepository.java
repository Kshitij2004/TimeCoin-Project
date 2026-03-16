package t_12.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Transaction;

/**
 * Data access methods for blockchain and purchase transaction records.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByTransactionHash(String transactionHash);
    List<Transaction> findBySenderAddress(String senderAddress);
    List<Transaction> findByReceiverAddress(String receiverAddress);
    List<Transaction> findByStatus(Transaction.Status status);
    List<Transaction> findByBlockId(Integer blockId);
    Page<Transaction> findByUserIdAndTransactionTypeInOrderByTimestampDescIdDesc(
            Integer userId,
            Collection<Transaction.TransactionType> transactionTypes,
            Pageable pageable
    );
    long countByUserIdAndTransactionTypeIn(
            Integer userId,
            Collection<Transaction.TransactionType> transactionTypes
    );
}
