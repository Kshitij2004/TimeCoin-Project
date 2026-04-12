package t_12.backend.api.auth;

/**
 * Response body returned on successful login, containing both the short-lived
 * access token and the long-lived refresh token.
 */
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;

    /**
     * Creates a login response with both issued tokens.
     *
     * @param accessToken signed JWT for authenticating requests
     * @param refreshToken opaque token for obtaining a new access token
     */
    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
