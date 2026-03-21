package t_12.backend.service;

import org.springframework.stereotype.Service;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.WalletRepository;

/**
 * Service class for handling wallet-related business logic. Provides methods to
 * retrieve wallet data for users.
 */
@Service
public class WalletService {

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
     * Retrieves the wallet for a specific sender address.
     *
     * @param senderAddress the wallet address of the sender
     * @return the Wallet entity for the address
     * @throws ResourceNotFoundException if no wallet is found for the address
     */
    public Wallet getWalletBySenderAddress(String senderAddress) {
        return walletRepository.findByWalletAddress(senderAddress)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet not found for address: " + senderAddress
        ));
    }
}
