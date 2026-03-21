package t_12.backend.api.marketplace;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.ListingService;

/**
 * REST controller exposing marketplace purchase endpoints.
 *
 * Base path: /api/listings
 */
@RestController
@RequestMapping("/api/listings")
public class MarketplaceController {

    private final ListingService listingService;

    public MarketplaceController(ListingService listingService) {
        this.listingService = listingService;
    }

    /**
     * Purchases a marketplace listing using TimeCoin.
     *
     * POST /api/listings/{id}/purchase
     *
     * Validates the buyer, checks their balance, transfers funds from buyer
     * to seller, creates a PENDING transaction, and marks the listing as SOLD.
     * The entire operation is atomic — it either fully succeeds or fully rolls back.
     *
     * @param id      the ID of the listing to purchase (path variable)
     * @param request request body containing the buyerUserId
     * @return 200 OK with the transaction hash and success message,
     *         or 400/404 with an error message if validation fails
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<PurchaseResponse> purchaseListing(
            @PathVariable Integer id,
            @RequestBody PurchaseRequest request) {

        String txHash = listingService.purchaseListing(id, request.getBuyerUserId());
        return ResponseEntity.ok(new PurchaseResponse(txHash, "Purchase successful"));
    }
}










