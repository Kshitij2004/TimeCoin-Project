package t_12.backend.api.auth;

/**
 * Response body returned on login.
 *
 * Normal login: accessToken + refreshToken are set, requires2FA is false.
 * 2FA required: requires2FA is true, tempToken is set, accessToken is null.
 * The client must POST /api/auth/2fa/verify with the tempToken + 6-digit code
 * to receive the real accessToken.
 */
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final boolean requires2FA;
    private final String tempToken;

    /**
     * Normal login response — no 2FA required.
     */
    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.requires2FA = false;
        this.tempToken = null;
    }

    /**
     * 2FA required response — client must verify the TOTP code.
     *
     * @param tempToken short-lived token identifying the pending session
     */
    public LoginResponse(String tempToken, boolean requires2FA) {
        this.accessToken = null;
        this.refreshToken = null;
        this.requires2FA = requires2FA;
        this.tempToken = tempToken;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public boolean isRequires2FA() { return requires2FA; }
    public String getTempToken() { return tempToken; }
}