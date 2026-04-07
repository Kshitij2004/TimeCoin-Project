package t_12.backend.api.auth;

/**
 * Represents the expected JSON body of the refresh token request.
 */
public class RefreshRequest {

    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
