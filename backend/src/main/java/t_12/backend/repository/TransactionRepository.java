package t_12.backend.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<Transaction> findBySenderAddressOrReceiverAddress(String senderAddress, String receiverAddress);
    boolean existsByTransactionHash(String transactionHash);




    /** Sum of amounts received by this address in confirmed transactions */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.receiverAddress = :address AND t.status = :status")
    BigDecimal sumAmountByReceiverAndStatus(@Param("address") String address,
                                            @Param("status") Transaction.Status status);

    /** Sum of amounts sent by this address in confirmed transactions */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.senderAddress = :address AND t.status = :status")
    BigDecimal sumAmountBySenderAndStatus(@Param("address") String address,
                                          @Param("status") Transaction.Status status);

    /** Sum of fees paid by this address in confirmed transactions */
    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM Transaction t " +
           "WHERE t.senderAddress = :address AND t.status = :status")
    BigDecimal sumFeesBySenderAndStatus(@Param("address") String address,
                                        @Param("status") Transaction.Status status);
}

