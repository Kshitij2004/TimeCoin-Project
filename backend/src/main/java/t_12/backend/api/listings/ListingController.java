package t_12.backend.api.listings;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.ListingService;

/**
 * Controller for marketplace listing endpoints. GET endpoints are public. POST,
 * PUT, and DELETE require authentication.
 */
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    /**
     * Creates a new listing for the authenticated seller.
     *
     * @param request the listing details
     * @return the created listing as a DTO
     */
    @PostMapping
    public ResponseEntity<ListingDTO> createListing(
            @RequestBody CreateListingRequest request) {
        Integer sellerId = getAuthenticatedUserId();
        return ResponseEntity.ok(
                new ListingDTO(listingService.createListing(request, sellerId)));
    }

    /**
     * Returns all active listings, optionally filtered by category.
     *
     * @param category optional category filter
     * @return list of active listings as DTOs
     */
    @GetMapping
    public ResponseEntity<List<ListingDTO>> getListings(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(
                listingService.getListings(category)
                        .stream()
                        .map(ListingDTO::new)
                        .toList());
    }

    /**
     * Returns a single listing by ID.
     *
     * @param id the listing ID
     * @return the listing as a DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> getListingById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                new ListingDTO(listingService.getListingById(id)));
    }

    /**
     * Updates an existing listing. Only the seller may update their own
     * listing.
     *
     * @param id the listing ID to update
     * @param request the updated fields
     * @return the updated listing as a DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ListingDTO> updateListing(
            @PathVariable Integer id,
            @RequestBody UpdateListingRequest request) {
        Integer sellerId = getAuthenticatedUserId();
        return ResponseEntity.ok(
                new ListingDTO(listingService.updateListing(id, request, sellerId)));
    }

    /**
     * Removes a listing by setting its status to REMOVED. Only the seller may
     * remove their own listing.
     *
     * @param id the listing ID to remove
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Integer id) {
        Integer sellerId = getAuthenticatedUserId();
        listingService.deleteListing(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the authenticated user's ID from the SecurityContext. Set by
     * AuthFilter after validating the JWT on each request.
     *
     * @return the authenticated user's ID
     */
    private Integer getAuthenticatedUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
