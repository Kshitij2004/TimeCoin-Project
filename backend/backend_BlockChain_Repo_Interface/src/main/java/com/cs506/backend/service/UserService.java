package com.cs506.backend.service;

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

import com.cs506.backend.entity.User;
import com.cs506.backend.entity.Wallet;
import com.cs506.backend.exception.DuplicateResourceException;
import com.cs506.backend.repository.UserRepository;
import com.cs506.backend.repository.WalletRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public UserService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
        this.secureRandom = new SecureRandom();
    }

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

        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setWalletAddress(generateWalletAddress(savedUser.getId(), username));
        wallet.setPublicKey(generatePublicKey(savedUser.getId(), email));
        wallet.setCoinBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        return savedUser;
    }

    private String generateWalletAddress(Integer userId, String username) {
        byte[] randomBytes = new byte[8];
        secureRandom.nextBytes(randomBytes);
        String entropy = userId + ":" + username + ":" + UUID.randomUUID() + ":" + HexFormat.of().formatHex(randomBytes);
        return "wlt_" + hash(entropy).substring(0, 40);
    }

    private String generatePublicKey(Integer userId, String email) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String entropy = userId + ":" + email + ":" + UUID.randomUUID() + ":" + HexFormat.of().formatHex(randomBytes);
        return "pub_" + hash(entropy);
    }

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
