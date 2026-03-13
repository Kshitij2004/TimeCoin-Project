package t_12.backend.api.auth;

import java.time.LocalDateTime;

import t_12.backend.entity.User;

/**
 * Data Transfer Object for User information, excluding sensitive data like
 * passwords.
 */
public class UserDTO {

    private final Integer id;
    private final String username;
    private final String email;
    private final LocalDateTime createdAt;

    /**
     * Constructs a UserDTO from a User entity.
     *
     * @param user the User entity to convert
     */
    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
