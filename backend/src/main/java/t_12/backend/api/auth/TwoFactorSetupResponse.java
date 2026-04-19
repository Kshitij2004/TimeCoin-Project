package t_12.backend.api.auth;

/**
 * Response for POST /api/auth/2fa/setup — contains the otpauth URI
 * to render as a QR code so the user can add CrypMart to their authenticator app.
 */
public class TwoFactorSetupResponse {

    private final String otpAuthUri;

    public TwoFactorSetupResponse(String otpAuthUri) {
        this.otpAuthUri = otpAuthUri;
    }

    public String getOtpAuthUri() {
        return otpAuthUri;
    }
}