package t_12.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.api.listings.CreateListingRequest;
import t_12.backend.entity.Listing;
import t_12.backend.repository.ListingRepository;

/**
 * Unit tests for ListingService class. Tests listing CRUD operations and
 * authorization checks.
 */
@ExtendWith(MockitoExtension.class)
public class ListingsServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingService listingService;

    /**
     * Tests that createListing saves and returns a listing with ACTIVE status
     * and the correct sellerId.
     */
    @Test
    void CreateListing_ReturnsListing_WhenValidInputTest() {
        CreateListingRequest request = new CreateListingRequest();
        request.setTitle("Test Listing");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("5.00000000"));
        request.setCategory("Test Category");
        request.setImageUrl("http://example.com/image.jpg");

        Listing savedListing = new Listing();
        savedListing.setId(1);
        savedListing.setSellerId(9);
        savedListing.setTitle(request.getTitle());
        savedListing.setDescription(request.getDescription());
        savedListing.setPrice(request.getPrice());
        savedListing.setCategory(request.getCategory());
        savedListing.setImageUrl(request.getImageUrl());
        savedListing.setStatus(Listing.Status.ACTIVE);

        when(listingRepository.save(any(Listing.class)))
                .thenReturn(savedListing);

        Listing result = listingService.createListing(request, 9);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(9, result.getSellerId());
        assertEquals("Test Listing", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(new BigDecimal("5.00000000"), result.getPrice());
        assertEquals("Test Category", result.getCategory());
        assertEquals("http://example.com/image.jpg", result.getImageUrl());
        assertEquals(Listing.Status.ACTIVE, result.getStatus());
        verify(listingRepository, times(1)).save(any(Listing.class));
    }

    /**
     * Tests that getListings returns all active listings when no category
     * filter is provided.
     */
    @Test
    void GetListings_ReturnsActiveListings_WhenNoCategoryFilterTest() {
        Listing listing1 = new Listing();
        listing1.setStatus(Listing.Status.ACTIVE);
        Listing listing2 = new Listing();
        listing2.setStatus(Listing.Status.ACTIVE);
        when(listingRepository.findByStatus(Listing.Status.ACTIVE))
                .thenReturn(List.of(listing1, listing2));

        java.util.List<Listing> result = listingService.getListings(null);
        assertEquals(2, result.size());
        verify(listingRepository, times(1)).findByStatus(Listing.Status.ACTIVE);
    }

    /**
     * Tests that getListings filters by category when a category filter is
     * provided.
     */
    @Test
    void GetListings_ReturnsFilteredListings_WhenCategoryFilterTest() {
        Listing listing1 = new Listing();
        listing1.setStatus(Listing.Status.ACTIVE);
        listing1.setCategory("Test Category");
        Listing listing2 = new Listing();
        listing2.setStatus(Listing.Status.ACTIVE);
        listing2.setCategory("Other Category");
        when(listingRepository.findByStatusAndCategory(
                Listing.Status.ACTIVE, "Test Category"))
                .thenReturn(List.of(listing1));

        java.util.List<Listing> result = listingService.getListings("Test Category");
        assertEquals(1, result.size());
        assertEquals("Test Category", result.get(0).getCategory());
        verify(listingRepository, times(1)).findByStatusAndCategory(
                Listing.Status.ACTIVE, "Test Category");
    }

    /**
     * Tests that getListingById returns the listed listing when it exists.
     */
    @Test
    void GetListingById_ReturnsListing_WhenListingExistsTest() {
        Listing listing = new Listing();
        listing.setId(1);
        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(listing));

        Listing result = listingService.getListingById(1);
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(listingRepository, times(1)).findById(1);
    }

    /**
     * Tests that getListingById throws ResourceNotFoundException when listing
     * does not exist.
     */
    @Test
    void GetListingById_ThrowsException_WhenListingNotFoundTest() {
        when(listingRepository.findById(1)).thenReturn(java.util.Optional.empty());

        assertThrows(t_12.backend.exception.ResourceNotFoundException.class, () -> {
            listingService.getListingById(1);
        });
        verify(listingRepository, times(1)).findById(1);
    }

    /**
     * Tests that updateListing saves and returns the updated listing when the
     * requester is the owner.
     */
    @Test
    void UpdateListing_ReturnsUpdatedListing_WhenOwnerTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);

        t_12.backend.api.listings.UpdateListingRequest request
                = new t_12.backend.api.listings.UpdateListingRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setPrice(new BigDecimal("10.00000000"));
        request.setCategory("Updated Category");
        request.setImageUrl("http://example.com/updated.jpg");
        request.setStatus(Listing.Status.ACTIVE);

        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(existing));
        when(listingRepository.save(any(Listing.class)))
                .thenReturn(existing);

        Listing result = listingService.updateListing(1, request, 9);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(listingRepository, times(1))
                .save(any(Listing.class));
    }

    /**
     * Tests that updateListing throws ForbiddenException when the requester is
     * not the owner.
     */
    @Test
    void UpdateListing_ThrowsException_WhenNotOwnerTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);

        t_12.backend.api.listings.UpdateListingRequest request
                = new t_12.backend.api.listings.UpdateListingRequest();

        when(listingRepository.findById(1))
                .thenReturn(java.util.Optional.of(existing));

        assertThrows(t_12.backend.exception.ForbiddenException.class,
                () -> listingService.updateListing(1, request, 99));
    }

    /**
     * Tests that deleteListing sets the listing status to REMOVED when the
     * requester is the owner.
     */
    @Test
    void DeleteListing_SetsStatusRemoved_WhenOwnerRequestsTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);
        existing.setStatus(Listing.Status.ACTIVE);

        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(existing));

        listingService.deleteListing(1, 9);

        assertEquals(Listing.Status.REMOVED, existing.getStatus());
        verify(listingRepository, times(1)).save(existing);
    }

    /**
     * Tests that deleteListing throws ForbiddenException when a non-owner
     * requests.
     */
    @Test
    void DeleteListing_ThrowsException_WhenNonOwnerRequestsTest() {
        Listing existing = new Listing();
        existing.setId(1);
        existing.setSellerId(9);

        when(listingRepository.findById(1)).thenReturn(java.util.Optional.of(existing));

        assertThrows(
                t_12.backend.exception.ForbiddenException.class,
                () -> listingService.deleteListing(1, 99)
        );
    }
}
