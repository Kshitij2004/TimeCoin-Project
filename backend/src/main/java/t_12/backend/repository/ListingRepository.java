package t_12.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Listing;

/**
 * Repository for accessing and modifying Listing records in the database.
 * Extends JpaRepository to provide standard CRUD operations.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {

    /**
     * Finds a listing by its primary key.
     *
     * @param id the listing ID
     * @return an Optional containing the listing if found, or empty if not
     */
    Optional<Listing> findById(Integer id);
}