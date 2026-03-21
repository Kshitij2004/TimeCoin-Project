package t_12.backend.service;

import t_12.backend.entity.User;
import t_12.backend.entity.Wallet;

/**
 * Encapsulates user registration output and the newly created wallet metadata.
 */
public class UserRegistrationResult {

    private final User user;
    private final Wallet wallet;
    private final String privateKey;

    /**
     * Creates a registration result instance.
     *
     * @param user the newly created user
     * @param wallet the wallet created for the user
     * @param privateKey the generated private key for one-time delivery
     */
    public UserRegistrationResult(User user, Wallet wallet, String privateKey) {
        this.user = user;
        this.wallet = wallet;
        this.privateKey = privateKey;
    }

    /**
     * Returns the registered user.
     *
     * @return the created user entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the wallet created during registration.
     *
     * @return the created wallet entity
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Returns the one-time private key generated for wallet creation.
     *
     * @return the private key string
     */
    public String getPrivateKey() {
        return privateKey;
    }
}
