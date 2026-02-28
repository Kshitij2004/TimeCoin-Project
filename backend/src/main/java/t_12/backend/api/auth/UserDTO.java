package t_12.backend.api.auth;

import java.time.LocalDateTime;

import t_12.backend.entity.User;

public class UserDTO {

    private Integer id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

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