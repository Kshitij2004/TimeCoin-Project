package t_12.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.BlockTransaction;

/**
 * Data access methods for block-to-transaction join rows.
 */
@Repository
public interface BlockTransactionRepository extends JpaRepository<BlockTransaction, Integer> {
    List<BlockTransaction> findByBlockIdOrderByIdAsc(Integer blockId);
    List<BlockTransaction> findByBlockId(Integer blockId);
    List<BlockTransaction> findByTransactionId(Integer transactionId);
}
