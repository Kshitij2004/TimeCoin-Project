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

    /**
     * Finds all listings posted by a specific seller.
     *
     * @param sellerId the ID of the seller
     * @return list of listings belonging to that seller
     */
    List<Listing> findBySellerId(Integer sellerId);

    /**
     * Finds all listings with a given status.
     *
     * @param status the listing status to filter by
     * @return list of listings with that status
     */
    List<Listing> findByStatus(Listing.Status status);

    /**
     * Finds listings matching both a status and category.
     *
     * @param status the listing status to filter by
     * @param category the category to filter by
     * @return list of listings matching both criteria
     */
    List<Listing> findByStatusAndCategory(Listing.Status status, String category);
}