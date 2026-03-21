package t_12.backend.api.balance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.exception.ResourceNotFoundException;
import t_12.backend.service.BalanceService;

/**
 * REST controller for wallet balance queries. Returns available,
 * staked, and total balances derived from confirmed chain state.
 */
@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /**
     * GET /api/balances/{walletAddress}
     * Returns the ledger-derived balance breakdown for a wallet.
     */
    @GetMapping("/{walletAddress}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String walletAddress) {
        try {
            BalanceResponse balance = balanceService.getBalance(walletAddress);
            return ResponseEntity.ok(balance);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}