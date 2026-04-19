package t_12.backend.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import t_12.backend.api.auth.LoginResponse;
import t_12.backend.entity.RefreshToken;
import t_12.backend.entity.User;
import t_12.backend.exception.ApiException;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;

/**
 * Service class for handling user-related business logic. Manages user
 * registration, validation, wallet creation, and 2FA.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String secretKey;
    private final int expiryHours;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorService twoFactorService;

    // Temporary in-memory store for pending 2FA sessions.
    // Maps tempToken -> userId. Entries expire after 5 minutes (checked on access).
    // Key: tempToken, Value: [userId, expiryEpochSeconds]
    private final ConcurrentHashMap<String, long[]> pending2FASessions = new ConcurrentHashMap<>();

    public UserService(
            UserRepository userRepository,
            WalletService walletService,
            RefreshTokenService refreshTokenService,
            TwoFactorService twoFactorService,
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiry-hours}") int expiryHours) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.refreshTokenService = refreshTokenService;
        this.twoFactorService = twoFactorService;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
        this.secretKey = secretKey;
        this.expiryHours = expiryHours;
    }

    public User register(String username, String email, String password) {
        return registerWithWallet(username, email, password).getUser();
    }

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
     * Authenticates a user. If 2FA is enabled, returns a temp token instead
     * of a real JWT — the client must verify the TOTP code to complete login.
     */
    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // If 2FA is enabled, issue a temp token instead of the real JWT
        if (user.isTwoFactorEnabled()) {
            String tempToken = UUID.randomUUID().toString();
            long expiry = Instant.now().getEpochSecond() + 300; // 5 minutes
            pending2FASessions.put(tempToken, new long[]{user.getId(), expiry});
            return new LoginResponse(tempToken, true);
        }

        return issueTokens(user.getId());
    }

    /**
     * Generates a new TOTP secret and returns the otpauth URI for QR code display.
     * Does NOT enable 2FA yet — the user must verify a code first via confirmSetup2FA.
     *
     * @param userId the authenticated user's ID
     * @return otpauth:// URI to render as a QR code
     */
    public String setup2FA(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String secret = twoFactorService.generateSecret();
        user.setTwoFactorSecret(secret);
        // Not enabled yet — user must verify a code first
        userRepository.save(user);

        return twoFactorService.buildOtpAuthUri(secret, user.getUsername(), "CrypMart");
    }

    /**
     * Confirms 2FA setup by verifying the first code from the authenticator app.
     * Enables 2FA on the account once verified.
     *
     * @param userId the authenticated user's ID
     * @param code   the 6-digit TOTP code from the authenticator app
     */
    public void confirmSetup2FA(Integer userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getTwoFactorSecret() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "2FA setup not initiated");
        }

        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid 2FA code");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    /**
     * Verifies a TOTP code for a pending 2FA login session.
     * If valid, issues the real JWT and refresh token.
     *
     * @param tempToken  the temp token returned by login when 2FA was required
     * @param code       the 6-digit TOTP code from the authenticator app
     * @param trustDevice if true, note is included (future: persist device trust)
     * @return full LoginResponse with accessToken and refreshToken
     */
    public LoginResponse verify2FA(String tempToken, String code, boolean trustDevice) {
        long[] session = pending2FASessions.get(tempToken);

        if (session == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired 2FA session");
        }

        long expiry = session[1];
        if (Instant.now().getEpochSecond() > expiry) {
            pending2FASessions.remove(tempToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "2FA session expired. Please log in again.");
        }

        int userId = (int) session[0];
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid 2FA code");
        }

        pending2FASessions.remove(tempToken);
        return issueTokens(userId);
    }

    /**
     * Issues a new access token from a valid refresh token.
     */
    public LoginResponse refresh(String refreshToken) {
        RefreshToken validated = refreshTokenService.validate(refreshToken);
        return issueTokens(validated.getUserId());
    }

    private LoginResponse issueTokens(int userId) {
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(expiryHours, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();

        String refreshToken = refreshTokenService.generate(userId).getToken();
        return new LoginResponse(accessToken, refreshToken);
    }
}
