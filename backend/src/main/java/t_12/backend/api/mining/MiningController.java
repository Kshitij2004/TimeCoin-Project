package t_12.backend.api.mining;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.service.MiningService;
import t_12.backend.service.WalletService;

/**
 * Handles mining endpoints. POST /api/mining/mine triggers a click for the
 * authenticated wallet. GET /api/mining/stats/{walletAddress} returns mining
 * stats for any wallet.
 */
@RestController
@RequestMapping("/api/mining")
public class MiningController {

    private final MiningService miningService;
    private final WalletService walletService;

    public MiningController(MiningService miningService, WalletService walletService) {
        this.miningService = miningService;
        this.walletService = walletService;
    }

    /**
     * Records a mining click for the authenticated wallet. Wallet address is
     * extracted from the JWT via SecurityContext — not accepted from the
     * request body to prevent spoofing.
     *
     * @return the updated accumulator row for the current window
     */
    @PostMapping("/mine")
    public ResponseEntity<MineResponse> mine() {
        Integer userId = (Integer) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        String walletAddress = walletService.getWalletByUserId(userId).getWalletAddress();

        return ResponseEntity.ok(miningService.mine(walletAddress));
    }

    /**
     * Returns mining stats for the given wallet address.
     *
     * @param walletAddress the wallet address to look up
     * @return MiningStatsResponse with totalMined, totalCount,
     * cooldownRemaining
     */
    @GetMapping("/stats/{walletAddress}")
    public ResponseEntity<MiningStatsResponse> getStats(
            @PathVariable String walletAddress) {
        return ResponseEntity.ok(miningService.getStats(walletAddress));
    }
}
