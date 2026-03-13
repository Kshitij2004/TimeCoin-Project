package t_12.backend.api.auth;

/**
 * Request body used by the registration endpoint.
 */
public class RegisterRequest {
    // Desired public username for the new account.
    private String username;
    // Email used as a unique login/contact identifier.
    private String email;
    // Raw password received from the client before hashing.
    private String password;

    /**
     * Returns the username provided in the request.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Stores the username provided by the client.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the email provided in the request.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Stores the email provided by the client.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the raw password from the request body.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Stores the raw password before the service hashes it.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
