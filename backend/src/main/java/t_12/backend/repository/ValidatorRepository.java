package t_12.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Validator;

/**
 * Data access methods for validator records.
 */
@Repository
public interface ValidatorRepository extends JpaRepository<Validator, Integer> {
    Optional<Validator> findByWalletAddress(String walletAddress);
    List<Validator> findByStatus(Validator.Status status);
}
