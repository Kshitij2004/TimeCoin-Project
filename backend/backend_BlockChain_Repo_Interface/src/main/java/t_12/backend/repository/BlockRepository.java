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
    // Finds a block by its position in the chain.
    Optional<Block> findByBlockHeight(Integer blockHeight);
    // Finds a block by its unique block hash.
    Optional<Block> findByBlockHash(String blockHash);
    // Returns all blocks with the requested lifecycle status.
    List<Block> findByStatus(Block.Status status);
    // Returns all blocks produced by a specific validator address.
    List<Block> findByValidatorAddress(String validatorAddress);
}
