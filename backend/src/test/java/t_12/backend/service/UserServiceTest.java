package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import t_12.backend.api.auth.LoginResponse;
import t_12.backend.entity.RefreshToken;
import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.ApiException;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;

/**
 * Unit tests for UserService class. Tests user registration functionality
 * including validation, success, and error cases.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TwoFactorService twoFactorService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                walletService,
                refreshTokenService,
                twoFactorService,
                "test-secret-key-that-is-long-enough-32chars",
                1
        );
    }

    /**
     * Tests successful user registration with valid input. Verifies user and
     * wallet creation, password hashing, and repository interactions.
     */
    @Test
    void Register_ReturnsUser_WhenValidInputTest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);

        // Mock 2FA secret generation (called during registration)
        when(twoFactorService.generateSecret()).thenReturn("TESTBASE32SECRET");

        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@email.com");
        savedUser.setPasswordHash("hashedpassword");
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setTwoFactorSecret("TESTBASE32SECRET");
        savedUser.setTwoFactorEnabled(true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Wallet savedWallet = new Wallet();
        savedWallet.setUserId(1);
        savedWallet.setWalletAddress("wlt_test");
        savedWallet.setPublicKey("public_key_test");
        savedWallet.setCoinBalance(BigDecimal.ZERO);
        when(walletService.createWalletForUser(1))
                .thenReturn(new WalletCreationResult(savedWallet, "private_key_test"));

        User result = userService.register("testuser", "test@email.com", "password123");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@email.com", result.getEmail());

        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@email.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(walletService, times(1)).createWalletForUser(1);
        verify(twoFactorService, times(1)).generateSecret();
    }

    /**
     * Tests that registration throws DuplicateResourceException when username
     * is already taken.
     */
    @Test
    void Register_ThrowsException_WhenUsernameTakenTest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.register("testuser", "test@email.com", "password123");
        });

        verify(userRepository, never()).save(any(User.class));
        verify(walletService, never()).createWalletForUser(any());
    }

    /**
     * Tests that registration throws DuplicateResourceException when email
     * is already taken.
     */
    @Test
    void Register_ThrowsException_WhenEmailTakenTest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.register("testuser", "test@email.com", "password123");
        });

        verify(userRepository, never()).save(any(User.class));
        verify(walletService, never()).createWalletForUser(any());
    }

    /**
     * Tests successful login when 2FA is disabled — returns real tokens directly.
     */
    @Test
    void Login_ReturnsTokens_WhenValidCredentialsAndNo2FATest() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("password123");

        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPasswordHash(hash);
        user.setTwoFactorEnabled(false); // 2FA off for this test

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("uuid-refresh-token");
        refreshToken.setUserId(1);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(userRepository.findByUsername("testuser"))
                .thenReturn(java.util.Optional.of(user));
        when(refreshTokenService.generate(1)).thenReturn(refreshToken);

        LoginResponse response = userService.login("testuser", "password123");

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assert (!response.getAccessToken().isBlank());
        assertEquals("uuid-refresh-token", response.getRefreshToken());
    }

    /**
     * Tests that login returns requires2FA=true when 2FA is enabled.
     */
    @Test
    void Login_Returns2FARequired_When2FAEnabledTest() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("password123");

        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPasswordHash(hash);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("TESTBASE32SECRET");

        when(userRepository.findByUsername("testuser"))
                .thenReturn(java.util.Optional.of(user));

        LoginResponse response = userService.login("testuser", "password123");

        assertNotNull(response);
        assert (response.isRequires2FA());
        assertNotNull(response.getTempToken());
    }

    /**
     * Tests that login throws RuntimeException when username does not exist.
     */
    @Test
    void Login_ThrowsException_WhenUsernameNotFoundTest() {
        when(userRepository.findByUsername("unknownuser"))
                .thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            userService.login("unknownuser", "password123");
        });
    }

    /**
     * Tests that login throws RuntimeException when password is incorrect.
     */
    @Test
    void Login_ThrowsException_WhenWrongPasswordTest() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("password123");

        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPasswordHash(hash);
        when(userRepository.findByUsername("testuser"))
                .thenReturn(java.util.Optional.of(user));

        assertThrows(RuntimeException.class, () -> {
            userService.login("testuser", "wrongpassword");
        });
    }

    /**
     * Tests that refresh returns new tokens when given a valid refresh token.
     */
    @Test
    void Refresh_ReturnsNewTokens_WhenValidRefreshTokenTest() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUserId(1);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken("new-refresh-token");
        newRefreshToken.setUserId(1);
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenService.validate("valid-refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.generate(1)).thenReturn(newRefreshToken);

        LoginResponse response = userService.refresh("valid-refresh-token");

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    /**
     * Tests that refresh throws ApiException when given an invalid refresh token.
     */
    @Test
    void Refresh_ThrowsException_WhenInvalidRefreshTokenTest() {
        when(refreshTokenService.validate("bad-token"))
                .thenThrow(new ApiException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"));

        assertThrows(ApiException.class, () -> {
            userService.refresh("bad-token");
        });
    }
}
