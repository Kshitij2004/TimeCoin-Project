package t_12.backend.api.listings;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import t_12.backend.entity.Listing;

/**
 * Response body representing a marketplace listing returned to the client.
 * Constructed from a Listing entity.
 */
public class ListingDTO {

    private final Integer id;
    private final Integer sellerId;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final String category;
    private final Listing.Status status;
    private final String imageUrl;
    private final LocalDateTime createdAt;

    /**
     * Constructs a ListingDTO from a Listing entity.
     *
     * @param listing the Listing entity to convert into a DTO
     */
    public ListingDTO(Listing listing) {
        this.id = listing.getId();
        this.sellerId = listing.getSellerId();
        this.title = listing.getTitle();
        this.description = listing.getDescription();
        this.price = listing.getPrice();
        this.category = listing.getCategory();
        this.status = listing.getStatus();
        this.imageUrl = listing.getImageUrl();
        this.createdAt = listing.getCreatedAt();
    }

    public Integer getId() {
        return id;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public Listing.Status getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
