package t_12.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Wallet;

/**
 * Data access methods for wallet records.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    // Finds the wallet owned by a given user id.
    Optional<Wallet> findByUserId(Integer userId);
    // Finds a wallet by its public address.
    Optional<Wallet> findByWalletAddress(String walletAddress);
    // Finds a wallet by its public key.
    Optional<Wallet> findByPublicKey(String publicKey);
}
