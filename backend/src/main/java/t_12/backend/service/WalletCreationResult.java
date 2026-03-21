package t_12.backend.service;

import t_12.backend.entity.Wallet;

/**
 * Encapsulates wallet creation output, including the one-time private key.
 */
public class WalletCreationResult {

    private final Wallet wallet;
    private final String privateKey;

    /**
     * Creates a new wallet creation result.
     *
     * @param wallet the created wallet entity
     * @param privateKey the generated private key (returned only at creation time)
     */
    public WalletCreationResult(Wallet wallet, String privateKey) {
        this.wallet = wallet;
        this.privateKey = privateKey;
    }

    /**
     * Returns the created wallet.
     *
     * @return the created wallet entity
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Returns the generated private key.
     *
     * @return the private key for one-time return
     */
    public String getPrivateKey() {
        return privateKey;
    }
}
