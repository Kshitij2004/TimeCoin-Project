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
}
