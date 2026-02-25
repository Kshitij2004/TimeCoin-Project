package t_12.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Coin;

@Repository
public interface CoinRepository extends JpaRepository<Coin, Long> {
}