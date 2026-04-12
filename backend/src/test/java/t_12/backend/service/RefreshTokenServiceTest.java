package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import t_12.backend.entity.RefreshToken;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 7);
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiryDays", 7);
    }

    @Test
    void generate_savesTokenForUser() {
        doNothing().when(refreshTokenRepository).deleteByUserId(1);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = refreshTokenService.generate(1);

        assertNotNull(result.getToken());
        assertEquals(1, result.getUserId());
        assertNotNull(result.getExpiresAt());
        // Expiry should be in the future.
        assert (result.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void generate_deletesExistingTokenFirst() {
        doNothing().when(refreshTokenRepository).deleteByUserId(1);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.generate(1);

        verify(refreshTokenRepository).deleteByUserId(1);
    }

    @Test
    void validate_returnsToken_whenValid() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setUserId(1);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validate("valid-token");

        assertEquals("valid-token", result.getToken());
        assertEquals(1, result.getUserId());
    }

    @Test
    void validate_throws_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.validate("bad-token"));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void validate_throws_whenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired-token");
        token.setUserId(1);
        // Set expiry in the past.
        token.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.validate("expired-token"));

        assertEquals("Refresh token has expired", ex.getMessage());
    }
}
