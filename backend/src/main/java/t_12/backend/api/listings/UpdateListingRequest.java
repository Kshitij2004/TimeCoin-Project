package t_12.backend.api.listings;

import java.math.BigDecimal;

import t_12.backend.entity.Listing;

/**
 * Request body for updating an existing listing. Only seller-editable fields
 * are exposed. sellerId and createdAt are immutable after creation.
 */
public class UpdateListingRequest {

    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private Listing.Status status;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public Listing.Status getStatus() {
        return status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setStatus(Listing.Status status) {
        this.status = status;
    }
}
