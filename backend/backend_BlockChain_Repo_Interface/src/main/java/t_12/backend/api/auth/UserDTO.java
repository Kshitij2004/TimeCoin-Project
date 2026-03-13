package t_12.backend.api.auth;

import t_12.backend.entity.User;

/**
 * Response DTO that exposes non-sensitive user fields.
 */
public class UserDTO {
    // Database identifier for the user.
    private Integer id;
    // Public username shown to other users.
    private String username;
    // Email returned to confirm the saved account.
    private String email;

    /**
     * Copies safe fields from a persisted user entity.
     */
    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
    }

    /**
     * Returns the saved user id.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the user's public username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the user's email address.
     */
    public String getEmail() {
        return email;
    }
}
