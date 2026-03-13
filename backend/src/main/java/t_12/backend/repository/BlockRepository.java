package t_12.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Block;

/**
 * Data access methods for block records.
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Integer> {
    Optional<Block> findByBlockHeight(Integer blockHeight);
    Optional<Block> findByBlockHash(String blockHash);
    List<Block> findByStatus(Block.Status status);
    List<Block> findByValidatorAddress(String validatorAddress);
}
