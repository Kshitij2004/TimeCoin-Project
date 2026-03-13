package t_12.backend.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Handles user registration and creation of the wallet paired with each user.
 */
@Service
public class UserService {

    // Persists and queries user records.
    private final UserRepository userRepository;
    // Persists and queries wallet records created for users.
    private final WalletRepository walletRepository;
    // Hashes incoming passwords before storage.
    private final BCryptPasswordEncoder passwordEncoder;
    // Provides entropy for generated wallet identifiers.
    private final SecureRandom secureRandom;

    /**
     * Builds the service with repository dependencies and crypto helpers.
     */
    public UserService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Creates a user, hashes their password, and provisions an empty wallet.
     */
    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Every new user gets a wallet immediately so other blockchain features can use it.
        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setWalletAddress(generateWalletAddress(savedUser.getId(), username));
        wallet.setPublicKey(generatePublicKey(savedUser.getId(), email));
        wallet.setCoinBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        return savedUser;
    }

    /**
     * Derives a readable wallet address from stable user data plus random entropy.
     */
    private String generateWalletAddress(Integer userId, String username) {
        byte[] randomBytes = new byte[8];
        secureRandom.nextBytes(randomBytes);
        String entropy = userId + ":" + username + ":" + UUID.randomUUID() + ":" + HexFormat.of().formatHex(randomBytes);
        return "wlt_" + hash(entropy).substring(0, 40);
    }

    /**
     * Derives a longer public key string for the user's wallet.
     */
    private String generatePublicKey(Integer userId, String email) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String entropy = userId + ":" + email + ":" + UUID.randomUUID() + ":" + HexFormat.of().formatHex(randomBytes);
        return "pub_" + hash(entropy);
    }

    /**
     * Produces a SHA-256 hex digest for generated identifiers.
     */
    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
