package t_12.backend.api.auth;

import java.time.LocalDateTime;

import t_12.backend.service.UserRegistrationResult;

/**
 * Registration response DTO that includes user metadata and wallet identity.
 */
public class RegisterResponseDTO {

    private final Integer id;
    private final String username;
    private final String email;
    private final LocalDateTime createdAt;
    private final String walletAddress;
    private final String publicKey;
    private final String privateKey;

    /**
     * Creates a registration response from a registration result.
     *
     * @param registrationResult result returned by registration service
     * @param includePrivateKey true when private key should be returned
     */
    public RegisterResponseDTO(
            UserRegistrationResult registrationResult,
            boolean includePrivateKey) {
        this.id = registrationResult.getUser().getId();
        this.username = registrationResult.getUser().getUsername();
        this.email = registrationResult.getUser().getEmail();
        this.createdAt = registrationResult.getUser().getCreatedAt();
        this.walletAddress = registrationResult.getWallet().getWalletAddress();
        this.publicKey = registrationResult.getWallet().getPublicKey();
        this.privateKey = includePrivateKey ? registrationResult.getPrivateKey() : null;
    }

    /**
     * Returns the created user ID.
     *
     * @return user ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the created username.
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the created email.
     *
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the registration timestamp.
     *
     * @return created timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the generated wallet address.
     *
     * @return wallet address
     */
    public String getWalletAddress() {
        return walletAddress;
    }

    /**
     * Returns the generated public key.
     *
     * @return public key
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Returns the generated private key when enabled for creation response.
     *
     * @return private key or null
     */
    public String getPrivateKey() {
        return privateKey;
    }
}
