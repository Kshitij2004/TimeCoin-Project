package t_12.backend.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import t_12.backend.entity.User;
import t_12.backend.exception.ApiException;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;

/**
 * Service class for handling user-related business logic. Manages user
 * registration, validation, and wallet creation.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String secretKey;
    private final int expiryHours;

    /**
     * Creates a user service with repository and wallet service dependencies.
     *
     * @param userRepository repository for user persistence and lookups
     * @param walletService service that manages wallet creation and identity
     * key generation for users
     * @param secretKey the secret key used for signing JWT tokens (injected
     * from properties)
     * @param expiryHours the number of hours before a JWT token expires
     * (injected from properties)
     */
    public UserService(
            UserRepository userRepository,
            WalletService walletService,
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiry-hours}") int expiryHours) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
        this.secretKey = secretKey;
        this.expiryHours = expiryHours;
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
        return registerWithWallet(username, email, password).getUser();
    }

    /**
     * Registers a new user and creates an associated wallet with generated
     * keypair.
     *
     * @param username the desired username (must be unique)
     * @param email the user's email address (must be unique)
     * @param password the raw password (will be hashed before storage)
     * @return a registration result containing user, wallet, and one-time
     * private key
     * @throws DuplicateResourceException if username or email already exists
     */
    public UserRegistrationResult registerWithWallet(String username, String email, String password) {
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
        WalletCreationResult walletCreationResult = walletService.createWalletForUser(savedUser.getId());

        return new UserRegistrationResult(
                savedUser,
                walletCreationResult.getWallet(),
                walletCreationResult.getPrivateKey()
        );
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     *
     * @param username the username to authenticate
     * @param password the raw password to verify
     * @return a signed JWT string containing the user ID and expiry
     * @throws ApiException if the username is not found or password is
     * incorrect
     */
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(expiryHours, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();
    }
}
