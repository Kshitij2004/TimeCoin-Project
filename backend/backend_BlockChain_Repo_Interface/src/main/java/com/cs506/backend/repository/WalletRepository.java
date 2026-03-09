package com.cs506.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cs506.backend.entity.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    Optional<Wallet> findByUserId(Integer userId);
    Optional<Wallet> findByWalletAddress(String walletAddress);
    Optional<Wallet> findByPublicKey(String publicKey);
}
