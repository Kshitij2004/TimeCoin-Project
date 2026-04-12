package t_12.backend.api.auth;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;
import t_12.backend.service.UserRegistrationResult;
import t_12.backend.service.UserService;

/**
 * Unit tests for AuthController class. Tests registration and login responses.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    /**
     * Tests that register returns private key when expose flag is enabled.
     */
    @Test
    void Register_ReturnsPrivateKey_WhenExposureEnabledTest() {
        AuthController authController = new AuthController(userService, true);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(7);
        user.setUsername("testuser");
        user.setEmail("test@email.com");
        user.setCreatedAt(LocalDateTime.now());

        Wallet wallet = new Wallet();
        wallet.setUserId(7);
        wallet.setWalletAddress("wlt_test");
        wallet.setPublicKey("pub_test");
        wallet.setCoinBalance(BigDecimal.ZERO);

        when(userService.registerWithWallet("testuser", "test@email.com", "password123"))
                .thenReturn(new UserRegistrationResult(user, wallet, "private_test_key"));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7, response.getBody().getId());
        assertEquals("wlt_test", response.getBody().getWalletAddress());
        assertEquals("pub_test", response.getBody().getPublicKey());
        assertEquals("private_test_key", response.getBody().getPrivateKey());
        verify(userService, times(1)).registerWithWallet("testuser", "test@email.com", "password123");
    }

    /**
     * Tests that register hides private key when expose flag is disabled.
     */
    @Test
    void Register_HidesPrivateKey_WhenExposureDisabledTest() {
        AuthController authController = new AuthController(userService, false);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(8);
        user.setUsername("testuser");
        user.setEmail("test@email.com");
        user.setCreatedAt(LocalDateTime.now());

        Wallet wallet = new Wallet();
        wallet.setUserId(8);
        wallet.setWalletAddress("wlt_test_hidden");
        wallet.setPublicKey("pub_test_hidden");
        wallet.setCoinBalance(BigDecimal.ZERO);

        when(userService.registerWithWallet("testuser", "test@email.com", "password123"))
                .thenReturn(new UserRegistrationResult(user, wallet, "private_hidden_key"));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(8, response.getBody().getId());
        assertEquals("wlt_test_hidden", response.getBody().getWalletAddress());
        assertEquals("pub_test_hidden", response.getBody().getPublicKey());
        assertNull(response.getBody().getPrivateKey());
        verify(userService, times(1)).registerWithWallet("testuser", "test@email.com", "password123");
    }

    /**
     * Tests that login returns an HTTP 200 response with both JWT tokens.
     */
    @Test
    void Login_ReturnsTokens_WhenCredentialsValidTest() {
        AuthController authController = new AuthController(userService, true);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse loginResponse = new LoginResponse("jwt-access-token", "uuid-refresh-token");
        when(userService.login("testuser", "password123")).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-access-token", response.getBody().getAccessToken());
        assertEquals("uuid-refresh-token", response.getBody().getRefreshToken());
        verify(userService, times(1)).login("testuser", "password123");
    }
}
