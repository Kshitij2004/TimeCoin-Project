package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Unit tests for UserService class. Tests user registration functionality
 * including validation, success, and error cases.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private UserService userService;

    /**
     * Tests successful user registration with valid input. Verifies user and
     * wallet creation, password hashing, and repository interactions.
     */
    @Test
    void Register_ReturnsUser_WhenValidInputTest() {
        // Arrange; simulate no existing username or email
        when(userRepository.existsByUsername("testuser"))
                .thenReturn(false);
        when(userRepository.existsByEmail("test@email.com"))
                .thenReturn(false);

        // Simulate the repository saving and returning a user with an id
        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@email.com");
        savedUser.setPasswordHash("hashedpassword");
        savedUser.setCreatedAt(LocalDateTime.now());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Simulate wallet save returning a wallet
        Wallet savedWallet = new Wallet();
        savedWallet.setUserId(1);
        savedWallet.setCoinBalance(BigDecimal.ZERO);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        User result = userService.register(
                "testuser",
                "test@email.com",
                "password123"
        );

        // Assert;  user is returned with correct fields
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@email.com", result.getEmail());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

        // Verify the right methods were called the right number of times
        verify(userRepository, times(1))
                .existsByUsername("testuser");
        verify(userRepository, times(1))
                .existsByEmail("test@email.com");
        verify(userRepository, times(1))
                .save(any(User.class));
        verify(walletRepository, times(1))
                .save(walletCaptor.capture());

        Wallet createdWallet = walletCaptor.getValue();
        assertNotNull(createdWallet.getWalletAddress());
        assertNotNull(createdWallet.getPublicKey());
    }

    /**
     * Tests that registration throws DuplicateResourceException when username
     * is already taken. Verifies that no user or wallet is saved in this case.
     */
    @Test
    void Register_ThrowsException_WhenUsernameTakenTest() {
        // Simulate username already existing
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.register(
                    "testuser",
                    "test@email.com",
                    "password123"
            );
        });

        // Verify we never proceeded to save anything
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    /**
     * Tests that registration throws DuplicateResourceException when email is
     * already taken. Verifies that no user or wallet is saved in this case.
     */
    @Test
    void Register_ThrowsException_WhenEmailTakenTest() {
        // Simulate username being free but email already existing
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.register(
                    "testuser",
                    "test@email.com",
                    "password123");
        });

        // Verify we never proceeded to save anything
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    /**
     * Tests successful login with valid credentials. Verifies that a non-null
     * JWT token string is returned.
     */
    @Test
    void Login_ReturnsToken_WhenValidCredentialsTest() {
        // This is a real BCrypt hash of "password123". Needed because
        // UserService uses a real BCryptPasswordEncoder internally,
        // so we can't mock the hashing. The hash must be genuine.
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("password123");

        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPasswordHash(hash);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(java.util.Optional.of(user));

        String token = userService.login("testuser", "password123");

        // We verify the token is a non-null, non-empty string.
        // We don't check the exact value since it changes with each call
        // due to the timestamp claims inside it.
        assertNotNull(token);
        assert (!token.isBlank());
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
}
