package t_12.backend.service;

import t_12.backend.entity.Wallet;
import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.repository.WalletRepository;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet getWalletByUserId(Integer userId) {
        return walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet not found for userId: " + userId
            ));
    }
}