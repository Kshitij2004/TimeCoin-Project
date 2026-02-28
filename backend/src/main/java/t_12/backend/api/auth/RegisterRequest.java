package t_12.backend.api.auth;

// This class represents the expected JSON body of the registration request.
// When the client sends:
// { "username": "john", "email": "john@email.com", "password": "secret" }
// Spring will automatically map those fields to this object.
public class RegisterRequest {

    private String username;
    private String email;
    private String password;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}