package com.cs506.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cs506.backend.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByTransactionHash(String transactionHash);
    List<Transaction> findBySenderAddress(String senderAddress);
    List<Transaction> findByReceiverAddress(String receiverAddress);
    List<Transaction> findByStatus(Transaction.Status status);
    List<Transaction> findByBlockId(Integer blockId);
}
