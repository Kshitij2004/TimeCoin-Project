package t_12.backend.api.wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.WalletService;

/**
 * Controller for handling wallet-related HTTP requests.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Retrieves the wallet information for a specific user.
     *
     * @param userId the ID of the user whose wallet to retrieve
     * @return ResponseEntity containing the WalletDTO with wallet data
     */
    @GetMapping
    public ResponseEntity<WalletDTO> getWallet(@RequestParam Integer userId) {
        return ResponseEntity.ok(
                new WalletDTO(walletService.getWalletByUserId(userId))
        );
    }
}
