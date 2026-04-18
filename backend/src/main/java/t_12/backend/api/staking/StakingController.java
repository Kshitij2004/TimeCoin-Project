package t_12.backend.api.staking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.entity.Wallet;
import t_12.backend.service.StakingService;
import t_12.backend.service.WalletService;

/**
 * Endpoints for staking and unstaking TimeCoin.
 */
@RestController
@RequestMapping("/api/staking")
public class StakingController {

    private final StakingService stakingService;
    private final WalletService walletService;

    public StakingController(StakingService stakingService, WalletService walletService) {
        this.stakingService = stakingService;
        this.walletService = walletService;
    }

    /**
     * POST /api/staking/stake
     * Locks TC from the authenticated user's available balance.
     */
    @PostMapping("/stake")
    public ResponseEntity<StakingOverviewResponse> stake(@RequestBody StakeRequest request) {
        Wallet wallet = walletService.getWalletByUserId(getAuthenticatedUserId());
        return ResponseEntity.ok(stakingService.stake(wallet.getWalletAddress(), request.getAmount()));
    }

    /**
     * POST /api/staking/unstake
     * Releases locked TC back to available balance for the authenticated user.
     */
    @PostMapping("/unstake")
    public ResponseEntity<StakingOverviewResponse> unstake(@RequestBody UnstakeRequest request) {
        Wallet wallet = walletService.getWalletByUserId(getAuthenticatedUserId());
        return ResponseEntity.ok(stakingService.unstake(wallet.getWalletAddress(), request.getAmount()));
    }

    /**
     * GET /api/staking/{walletAddress}
     * Returns current staking balance and event history for the wallet.
     */
    @GetMapping("/{walletAddress}")
    public ResponseEntity<StakingOverviewResponse> getStaking(@PathVariable String walletAddress) {
        return ResponseEntity.ok(stakingService.getStakingOverview(walletAddress));
    }

    private Integer getAuthenticatedUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
