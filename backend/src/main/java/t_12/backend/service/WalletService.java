package t_12.backend.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.DuplicateResourceException;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.WalletRepository;

/**
 * Service class for handling wallet-related business logic.
 */
@Service
public class WalletService {

    private static final int MAX_KEYPAIR_GENERATION_ATTEMPTS = 10;
    private static final String WALLET_ADDRESS_PREFIX = "wlt_";

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Retrieves the wallet for a specific user by their ID.
     *
     * @param userId the ID of the user whose wallet to retrieve
     * @return the Wallet entity for the user
     * @throws ResourceNotFoundException if no wallet is found for the user
     */
    public Wallet getWalletByUserId(Integer userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet not found for userId: " + userId
        ));
    }

    /**
     * Retrieves a wallet by public wallet address.
     *
     * @param walletAddress wallet address to look up
     * @return matching wallet entity
     * @throws ResourceNotFoundException if no wallet is found for the address
     */
    public Wallet getWalletByAddress(String walletAddress) {
        return walletRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet not found for walletAddress: " + walletAddress
        ));
    }

    /**
     * Retrieves a wallet by sender address.
     *
     * @param senderAddress sender wallet address
     * @return matching wallet entity
     */
    public Wallet getWalletBySenderAddress(String senderAddress) {
        return getWalletByAddress(senderAddress);
    }

    /**
     * Retrieves a wallet by public key.
     *
     * @param publicKey public key to look up
     * @return matching wallet entity
     * @throws ResourceNotFoundException when no wallet exists for the key
     */
    public Wallet getWalletByPublicKey(String publicKey) {
        return walletRepository.findByPublicKey(publicKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet not found for publicKey"
        ));
    }

    /**
     * Creates a wallet for a user with a generated Ed25519 keypair and derived
     * address.
     *
     * @param userId the user ID that owns the wallet
     * @return wallet creation result including one-time private key
     * @throws DuplicateResourceException when a wallet already exists for user
     */
    @Transactional
    public WalletCreationResult createWalletForUser(Integer userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Wallet already exists for userId: " + userId);
        }

        WalletIdentity identity = generateUniqueWalletIdentity();

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setWalletAddress(identity.walletAddress());
        wallet.setPublicKey(identity.publicKey());
        wallet.setCoinBalance(new BigDecimal("2.00000000"));
        wallet.setCreatedAt(LocalDateTime.now());

        Wallet savedWallet = walletRepository.save(wallet);
        return new WalletCreationResult(savedWallet, identity.privateKey());
    }

    /**
     * Ensures a wallet has both public key and derived address.
     *
     * @param wallet wallet entity to validate/backfill
     * @return wallet entity with complete identity fields
     */
    @Transactional
    public Wallet ensureWalletIdentity(Wallet wallet) {
        if (hasText(wallet.getWalletAddress()) && hasText(wallet.getPublicKey())) {
            return wallet;
        }

        WalletIdentity identity = generateUniqueWalletIdentity();
        wallet.setWalletAddress(identity.walletAddress());
        wallet.setPublicKey(identity.publicKey());
        return walletRepository.save(wallet);
    }

    /**
     * Derives a deterministic wallet address by hashing the public key.
     *
     * @param publicKey base64-encoded public key
     * @return hash-derived wallet address
     */
    String deriveWalletAddressFromPublicKey(String publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getBytes(StandardCharsets.UTF_8));
            return WALLET_ADDRESS_PREFIX + HexFormat.of().formatHex(hash, 0, 20);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available in runtime", ex);
        }
    }

    /**
     * Generates a unique wallet identity tuple of public key, private key, and
     * derived address.
     *
     * @return generated wallet identity
     */
    private WalletIdentity generateUniqueWalletIdentity() {
        for (int attempt = 0; attempt < MAX_KEYPAIR_GENERATION_ATTEMPTS; attempt++) {
            WalletIdentity identity = generateWalletIdentity();
            boolean publicKeyExists = walletRepository.existsByPublicKey(identity.publicKey());
            boolean walletAddressExists = walletRepository.existsByWalletAddress(identity.walletAddress());
            if (!publicKeyExists && !walletAddressExists) {
                return identity;
            }
        }

        throw new IllegalStateException("Unable to generate a unique wallet identity");
    }

    /**
     * Generates an Ed25519 keypair and derives the wallet address from the
     * public key.
     *
     * @return generated wallet identity data
     */
    private WalletIdentity generateWalletIdentity() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            String walletAddress = deriveWalletAddressFromPublicKey(publicKey);
            return new WalletIdentity(publicKey, privateKey, walletAddress);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Ed25519 algorithm is not available in runtime", ex);
        }
    }

    /**
     * Checks whether a string is non-null and non-blank.
     *
     * @param value value to check
     * @return true when value contains visible characters
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Lightweight immutable holder for generated wallet identity parts.
     *
     * @param publicKey base64-encoded public key
     * @param privateKey base64-encoded private key
     * @param walletAddress hash-derived public address
     */
    private record WalletIdentity(String publicKey, String privateKey, String walletAddress) {

    }
}
