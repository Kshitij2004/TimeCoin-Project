package t_12.backend.api.marketplace;

/**
 * Request body for purchasing a marketplace listing.
 *
 * Sent as JSON in the body of POST /api/listings/{id}/purchase.
 *
 * Example:
 * {
 *   "buyerUserId": 42
 * }
 *
 * TODO: Once authentication is fully wired up, buyerUserId should be
 * extracted from the JWT token rather than passed in the request body.
 */
public class PurchaseRequest {

    /** The ID of the user making the purchase. */
    private Integer buyerUserId;

    public Integer getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(Integer buyerUserId) { this.buyerUserId = buyerUserId; }
}