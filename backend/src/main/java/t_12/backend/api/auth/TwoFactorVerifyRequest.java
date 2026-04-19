package t_12.backend.api.auth;

/**
 * Request body for POST /api/auth/2fa/verify.
 * Sent after login when requires2FA is true.
 */
public class TwoFactorVerifyRequest {

    private String tempToken;
    private String code;
    private boolean trustDevice;

    public String getTempToken() { return tempToken; }
    public void setTempToken(String tempToken) { this.tempToken = tempToken; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public boolean isTrustDevice() { return trustDevice; }
    public void setTrustDevice(boolean trustDevice) { this.trustDevice = trustDevice; }
}