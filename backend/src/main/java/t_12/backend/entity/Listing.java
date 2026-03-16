package t_12.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Marketplace listing where a user posts goods/services priced in TimeCoin.
// References seller_id (user-level) rather than a wallet address because
// marketplace identity is the account, not a specific wallet.
@Entity
@Table(name = "listings")
public class Listing {

    // ACTIVE = visible and purchasable
    // SOLD = transaction completed, kept for history
    // REMOVED = seller pulled the listing
    public enum Status {
        ACTIVE, SOLD, REMOVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // priced in TimeCoin; 18,8 precision matches the wallet/transaction scale
    @Column(name = "price", nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // external URL; actual file uploads are handled by the frontend/CDN
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getters and setters

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

    public Status getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}