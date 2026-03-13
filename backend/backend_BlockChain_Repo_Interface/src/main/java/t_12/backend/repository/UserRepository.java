package t_12.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import t_12.backend.entity.User;

/**
 * Data access methods for application users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Checks whether a username is already taken.
    boolean existsByUsername(String username);
    // Checks whether an email address is already registered.
    boolean existsByEmail(String email);
}
