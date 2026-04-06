package t_12.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.entity.RefreshToken;
import t_12.backend.exception.ApiException;
import t_12.backend.repository.RefreshTokenRepository;

/**
 * Service for managing refresh tokens. Responsible for creating, validating,
 * and deleting refresh tokens. Each token is associated with a user and has an
 * expiration time. Tokens are stored in the database and deleted on use
 * (rotation) to prevent reuse.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final int refreshExpiryDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiry-days}") int refreshExpiryDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiryDays = refreshExpiryDays;
    }

    /**
     * Generates and persists a new refresh token for the given user. Deletes
     * any existing token for that user first (one active token per user).
     *
     * @param userId the ID of the user to issue a token for
     * @return the saved RefreshToken entity
     */
    @Transactional
    public RefreshToken generate(Integer userId) {
        // Rotate out any existing token for this user.
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshExpiryDays));
        refreshToken.setCreatedAt(LocalDateTime.now());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates an incoming refresh token string and returns the matching
     * entity. Throws if the token is not found or has expired.
     *
     * @param token the raw refresh token string sent by the client
     * @return the valid RefreshToken entity
     * @throws ApiException if the token is invalid or expired
     */
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        return refreshToken;
    }

    /**
     * Deletes all refresh tokens for the given user. Used during logout or
     * token invalidation.
     *
     * @param userId the user whose tokens should be revoked
     */
    @Transactional
    public void revokeByUserId(Integer userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
