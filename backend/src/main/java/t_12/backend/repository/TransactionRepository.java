package t_12.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Transaction;

/**
 * Repository for accessing and modifying Transaction records in the database.
 *
 * New transactions are inserted as PENDING status, effectively acting as
 * the mempool. The block assembler service will later query PENDING transactions
 * and package them into blocks, updating their status to CONFIRMED.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
}