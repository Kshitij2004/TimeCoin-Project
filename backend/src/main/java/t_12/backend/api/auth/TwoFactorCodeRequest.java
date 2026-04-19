
package t_12.backend.api.auth;

/**
 * Request body for POST /api/auth/2fa/confirm.
 * Used to verify the first code during 2FA setup.
 */
public class TwoFactorCodeRequest {

    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}