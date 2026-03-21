package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Service class for handling user-related business logic. Manages user
 * registration, validation, and wallet creation.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        // BCryptPasswordEncoder is the hashing algorithm we use on passwords.
        // A "strength" of 10 means it runs the hash function 2^10 = 1024 times,
        // making brute force attacks much harder. 10 is the industry standard default.
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    /**
     * Registers a new user with the provided details. Validates for duplicate
     * username/email, hashes the password, saves the user, and automatically
     * creates an associated wallet with zero balance.
     *
     * @param username the desired username (must be unique)
     * @param email the user's email address (must be unique)
     * @param password the raw password (will be hashed before storage)
     * @return the saved User entity with generated ID
     * @throws DuplicateResourceException if username or email already exists
     */
    public User register(String username, String email, String password) {

        // Duplicate Validation 
        // Before we do anything, check if the username or email is already taken.
        // We use existsBy instead of findBy here because we don't need the actual
        // user object, just a true/false answer. It's a cheaper database call.
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        // Build the User
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        // Never store a plain text password. passwordEncoder.encode() runs the
        // bcrypt algorithm on the raw password and returns a hashed string like:
        // "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
        // Even if someone got access to the database, they couldn't reverse it.
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        // Save the User
        // .save() persists the user to the database and returns the saved entity,
        // which now includes the auto-generated id we need for the wallet.
        User savedUser = userRepository.save(user);

        // Auto-create Wallet
        // Per the acceptance criteria, every new user gets a wallet with 0 balance.
        // We use the savedUser.getId() here because the wallet needs to know
        // which user it belongs to. This is the link between the two tables.
        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setWalletAddress(generateWalletAddress());
        wallet.setPublicKey(generatePublicKey());
        wallet.setCoinBalance(java.math.BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        return savedUser;
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     *
     * @param username the username to authenticate
     * @param password the raw password to verify
     * @return a signed JWT string containing the user ID and expiry
     * @throws RuntimeException if the username is not found or password is
     * incorrect
     */
    public String login(String username, String password) {

        // Look up the user. If they don't exist, fail immediately.
        // We use a generic message intentionally: never tell the caller
        // whether it was the username or password that was wrong, as that
        // leaks information to attackers.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // BCrypt's matches() re-hashes the submitted password and compares it
        // to the stored hash. We never decrypt! BCrypt is one-way.
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Build and sign the JWT.
        // The secret key is what makes the token tamper-proof. Only the
        // server knows it, so only the server can produce a valid signature.
        String secretKey = "your-super-secret-key-change-this-in-production";

        return Jwts.builder()
                // subject is the standard JWT field for "who this token is about"
                .subject(String.valueOf(user.getId()))
                // issuedAt and expiration are the standard fields for token lifetime
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();
    }

    private String generateWalletAddress() {
        return "addr_" + UUID.randomUUID();
    }

    private String generatePublicKey() {
        return "pub_" + UUID.randomUUID() + "_" + UUID.randomUUID();
    }
}
