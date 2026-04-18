package t_12.backend.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT t FROM Transaction t WHERE "
            + "(t.userId = :userId AND t.transactionType IN :purchaseTypes) "
            + "OR (t.transactionType IN :onChainTypes AND "
            + "    (t.receiverAddress = :walletAddress OR t.senderAddress = :walletAddress)) "
            + "ORDER BY t.timestamp DESC, t.id DESC")
    Page<Transaction> findWalletTransactions(
            @Param("userId") Integer userId,
            @Param("walletAddress") String walletAddress,
            @Param("purchaseTypes") Collection<Transaction.TransactionType> purchaseTypes,
            @Param("onChainTypes") Collection<Transaction.TransactionType> onChainTypes,
            Pageable pageable
    );

    List<Transaction> findBySenderAddressOrReceiverAddress(String senderAddress, String receiverAddress);

    boolean existsByTransactionHash(String transactionHash);

    /**
     * Sum of amounts received by this address in confirmed transactions
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.receiverAddress = :address AND t.status = :status")
    BigDecimal sumAmountByReceiverAndStatus(@Param("address") String address,
            @Param("status") Transaction.Status status);

    /**
     * Sum of amounts sent by this address in confirmed transactions
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.senderAddress = :address AND t.status = :status")
    BigDecimal sumAmountBySenderAndStatus(@Param("address") String address,
            @Param("status") Transaction.Status status);

    /**
     * Sum of fees paid by this address in confirmed transactions
     */
    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM Transaction t "
            + "WHERE t.senderAddress = :address AND t.status = :status")
    BigDecimal sumFeesBySenderAndStatus(@Param("address") String address,
            @Param("status") Transaction.Status status);

    /**
     * Sum of (amount + fee) for pending outgoing transactions from this address
     */
    @Query("SELECT COALESCE(SUM(t.amount + t.fee), 0) FROM Transaction t "
            + "WHERE t.senderAddress = :address AND t.status = 'PENDING'")
    BigDecimal sumPendingOutgoingByAddress(@Param("address") String address);

    // Total coinbase supply (for halving calculation)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.senderAddress IS NULL AND t.status = 'CONFIRMED'")
    BigDecimal sumConfirmedCoinbaseSupply();

    // Stats per wallet: total mined amount
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.senderAddress IS NULL "
            + "AND t.receiverAddress = :address AND t.status = 'CONFIRMED'")
    BigDecimal sumConfirmedCoinbaseByReceiver(@Param("address") String address);

    // Stats per wallet: total mined count
    @Query("SELECT COUNT(t) FROM Transaction t "
            + "WHERE t.senderAddress IS NULL "
            + "AND t.receiverAddress = :address AND t.status = 'CONFIRMED'")
    long countConfirmedCoinbaseByReceiver(@Param("address") String address);

    boolean existsBySenderAddressIsNullAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
            String receiverAddress,
            BigDecimal amount,
            BigDecimal fee,
            Integer nonce,
            Transaction.Status status
    );

    boolean existsBySenderAddressAndReceiverAddressAndAmountAndFeeAndNonceAndStatus(
            String senderAddress,
            String receiverAddress,
            BigDecimal amount,
            BigDecimal fee,
            Integer nonce,
            Transaction.Status status
    );

    long countByStatus(Transaction.Status status);
}
