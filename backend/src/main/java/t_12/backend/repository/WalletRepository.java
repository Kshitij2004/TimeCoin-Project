package t_12.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Wallet;

/**
 * Repository interface for Wallet entity database operations. Extends
 * JpaRepository to provide CRUD operations and custom query methods for wallet
 * data access.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    /**
     * Finds a wallet by the associated user ID.
     *
     * @param userId the ID of the user to find the wallet for
     * @return Optional containing the wallet if found, or empty if not found
     */
    Optional<Wallet> findByUserId(Integer userId);

    /**
     * Finds a wallet by its public address.
     *
     * @param walletAddress the wallet address to look up
     * @return Optional containing the wallet if found
     */
    Optional<Wallet> findByWalletAddress(String walletAddress);

    /**
     * Finds a wallet by its public key.
     *
     * @param publicKey the public key to look up
     * @return Optional containing the wallet if found
     */
    Optional<Wallet> findByPublicKey(String publicKey);

    /**
     * Checks whether a wallet already exists for a user.
     *
     * @param userId the user ID to check
     * @return true when a wallet already exists for the user
     */
    boolean existsByUserId(Integer userId);

    /**
     * Checks whether the wallet address is already used.
     *
     * @param walletAddress the wallet address to check
     * @return true when the address is already present
     */
    boolean existsByWalletAddress(String walletAddress);

    /**
     * Checks whether the public key is already used.
     *
     * @param publicKey the public key to check
     * @return true when the key is already present
     */
    boolean existsByPublicKey(String publicKey);
}
