package t_12.backend.api.listings;

import java.math.BigDecimal;

/**
 * Request body for creating a new marketplace listing. sellerId, status, and
 * createdAt are set server-side and must not be supplied by the client.
 */
public class CreateListingRequest {

    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;

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
}
