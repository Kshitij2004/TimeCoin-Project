package t_12.backend.api.wallet;

import t_12.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletDTO> getWallet(@RequestParam Integer userId) {
        return ResponseEntity.ok(new WalletDTO(walletService.getWalletByUserId(userId)));
    }
}