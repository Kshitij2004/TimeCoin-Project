package t_12.backend.api.auth;

import java.time.LocalDateTime;

import t_12.backend.service.UserRegistrationResult;

/**
 * Registration response DTO that includes user metadata, wallet identity,
 * and the otpauth URI for 2FA setup (2FA is enabled by default for all users).
 */
public class RegisterResponseDTO {

    private final Integer id;
    private final String username;
    private final String email;
    private final LocalDateTime createdAt;
    private final String walletAddress;
    private final String publicKey;
    private final String privateKey;
    private final String otpAuthUri;

    public RegisterResponseDTO(
            UserRegistrationResult registrationResult,
            boolean includePrivateKey,
            String otpAuthUri) {
        this.id = registrationResult.getUser().getId();
        this.username = registrationResult.getUser().getUsername();
        this.email = registrationResult.getUser().getEmail();
        this.createdAt = registrationResult.getUser().getCreatedAt();
        this.walletAddress = registrationResult.getWallet().getWalletAddress();
        this.publicKey = registrationResult.getWallet().getPublicKey();
        this.privateKey = includePrivateKey ? registrationResult.getPrivateKey() : null;
        this.otpAuthUri = otpAuthUri;
    }

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getWalletAddress() { return walletAddress; }
    public String getPublicKey() { return publicKey; }
    public String getPrivateKey() { return privateKey; }
    public String getOtpAuthUri() { return otpAuthUri; }
}
