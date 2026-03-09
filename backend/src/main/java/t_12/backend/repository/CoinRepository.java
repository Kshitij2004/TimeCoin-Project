package t_12.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Coin;

/**
 * Repository interface for Coin entity database operations. Provides CRUD
 * operations and custom query methods for coin data.
 */
@Repository
public interface CoinRepository extends JpaRepository<Coin, Long> {
}
