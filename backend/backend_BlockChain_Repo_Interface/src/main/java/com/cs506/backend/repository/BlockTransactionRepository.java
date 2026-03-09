package com.cs506.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cs506.backend.entity.BlockTransaction;

@Repository
public interface BlockTransactionRepository extends JpaRepository<BlockTransaction, Integer> {
    List<BlockTransaction> findByBlockId(Integer blockId);
    List<BlockTransaction> findByTransactionId(Integer transactionId);
}
