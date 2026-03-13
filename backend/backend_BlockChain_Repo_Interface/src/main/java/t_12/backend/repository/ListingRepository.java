package t_12.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.Listing;

/**
 * Data access methods for marketplace listings.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {
    // Returns listings created by the specified seller.
    List<Listing> findBySellerId(Integer sellerId);
    // Returns listings in the specified status.
    List<Listing> findByStatus(Listing.Status status);
}
