package t_12.backend.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.UserRegistrationResult;
import t_12.backend.service.UserService;

/**
 * Controller for handling authentication-related HTTP requests.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final boolean exposePrivateKeyOnCreate;

    public AuthController(
            UserService userService,
            @Value("${wallet.expose-private-key-on-create:true}") boolean exposePrivateKeyOnCreate) {
        this.userService = userService;
        this.exposePrivateKeyOnCreate = exposePrivateKeyOnCreate;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequest request) {
        UserRegistrationResult registrationResult = userService.registerWithWallet(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        String otpAuthUri = userService.getOtpAuthUri(registrationResult.getUser());

        RegisterResponseDTO dto = new RegisterResponseDTO(
                registrationResult,
                exposePrivateKeyOnCreate,
                otpAuthUri
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        LoginResponse response = userService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates 2FA setup for the authenticated user.
     * Returns an otpauth:// URI to be rendered as a QR code on the frontend.
     * 2FA is not enabled until /2fa/confirm is called with a valid code.
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2FA() {
        Integer userId = getAuthenticatedUserId();
        String otpAuthUri = userService.setup2FA(userId);
        return ResponseEntity.ok(new TwoFactorSetupResponse(otpAuthUri));
    }

    /**
     * Confirms 2FA setup by verifying the first code from the authenticator app.
     * Enables 2FA on the account once the code is valid.
     */
    @PostMapping("/2fa/confirm")
    public ResponseEntity<Void> confirm2FA(@RequestBody TwoFactorCodeRequest request) {
        Integer userId = getAuthenticatedUserId();
        userService.confirmSetup2FA(userId, request.getCode());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies a TOTP code during login when 2FA is required.
     * Accepts the tempToken from the login response and the 6-digit code.
     * Returns the real accessToken and refreshToken on success.
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verify2FA(@RequestBody TwoFactorVerifyRequest request) {
        LoginResponse response = userService.verify2FA(
                request.getTempToken(),
                request.getCode(),
                request.isTrustDevice()
        );
        return ResponseEntity.ok(response);
    }

    private Integer getAuthenticatedUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}