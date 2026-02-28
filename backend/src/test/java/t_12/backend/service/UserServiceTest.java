package t_12.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.repository.UserRepository;
import t_12.backend.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void Register_ReturnsUser_WhenValidInputTest() {
        // Arrange; simulate no existing username or email
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);

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
        User result = userService.register("testuser", "test@email.com", "password123");

        // Assert;  user is returned with correct fields
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@email.com", result.getEmail());

        // Verify the right methods were called the right number of times
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@email.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void Register_ThrowsException_WhenUsernameTakenTest() {
        // Simulate username already existing
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.register("testuser", "test@email.com", "password123");
        });

        // Verify we never proceeded to save anything
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void Register_ThrowsException_WhenEmailTakenTest() {
        // Simulate username being free but email already existing
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.register("testuser", "test@email.com", "password123");
        });

        // Verify we never proceeded to save anything
        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}