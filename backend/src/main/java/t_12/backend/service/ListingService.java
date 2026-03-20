package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import t_12.backend.api.listings.CreateListingRequest;
import t_12.backend.api.listings.UpdateListingRequest;
import t_12.backend.entity.Listing;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.ListingRepository;

/**
 * Service class for handling marketplace listing business logic. Manages
 * listing creation, retrieval, updates, and removal.
 */
@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    /**
     * Creates a new marketplace listing for the authenticated seller.
     *
     * @param request the listing details from the client
     * @param sellerId the ID of the authenticated user creating the listing
     * @return the saved Listing entity
     */
    public Listing createListing(CreateListingRequest request, Integer sellerId) {
        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategory(request.getCategory());
        listing.setImageUrl(request.getImageUrl());

        // New listings always start as ACTIVE.
        // Status can only change via updateListing or deleteListing.
        listing.setStatus(Listing.Status.ACTIVE);
        listing.setCreatedAt(LocalDateTime.now());

        return listingRepository.save(listing);
    }

    /**
     * Retrieves active listings, optionally filtered by category.
     *
     * @param category optional category filter; if null, returns all active
     * listings
     * @return list of matching active listings
     */
    public List<Listing> getListings(String category) {
        if (category != null && !category.isBlank()) {
            return listingRepository.findByStatusAndCategory(
                    Listing.Status.ACTIVE, category);
        }
        return listingRepository.findByStatus(Listing.Status.ACTIVE);
    }

    /**
     * Retrieves a single listing by its ID.
     *
     * @param id the listing ID
     * @return the Listing entity
     * @throws ResourceNotFoundException if no listing with that ID exists
     */
    public Listing getListingById(Integer id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Listing not found: " + id));
    }

    /**
     * Updates an existing listing. Only the seller who created the listing may
     * update it.
     *
     * @param id the listing ID to update
     * @param request the updated fields from the client
     * @param sellerId the ID of the authenticated user making the request
     * @return the updated Listing entity
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws RuntimeException if the requesting user is not the seller
     */
    public Listing updateListing(Integer id, UpdateListingRequest request,
            Integer sellerId) {
        Listing listing = getListingById(id);

        // Ownership check, only the seller can modify their own listing.
        // We compare the authenticated user's ID against the stored sellerId.
        if (!listing.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Forbidden: you do not own this listing");
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setCategory(request.getCategory());
        listing.setImageUrl(request.getImageUrl());
        listing.setStatus(request.getStatus());

        return listingRepository.save(listing);
    }

    /**
     * Removes a listing by setting its status to REMOVED. Only the seller who
     * created the listing may remove it. The row is kept for history.
     *
     * @param id the listing ID to remove
     * @param sellerId the ID of the authenticated user making the request
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws RuntimeException if the requesting user is not the seller
     */
    public void deleteListing(Integer id, Integer sellerId) {
        Listing listing = getListingById(id);

        // Ownership check. Same pattern as updateListing.
        if (!listing.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Forbidden: you do not own this listing");
        }

        // Soft delete. Set status to REMOVED rather than deleting the row.
        // This preserves listing history and matches the status transitions
        // defined in the issue.
        listing.setStatus(Listing.Status.REMOVED);
        listingRepository.save(listing);
    }
}
