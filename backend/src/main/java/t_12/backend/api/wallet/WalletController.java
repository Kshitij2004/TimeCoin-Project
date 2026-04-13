package t_12.backend.api.wallet;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.api.balance.BalanceResponse;
import t_12.backend.api.transaction.dto.TransactionHistoryResponseDTO;
import t_12.backend.entity.Wallet;
import t_12.backend.service.BalanceService;
import t_12.backend.service.TransactionHistoryService;
import t_12.backend.service.WalletService;

/**
 * Controller for handling wallet-related HTTP requests.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TransactionHistoryService transactionHistoryService;
    private final BalanceService balanceService;

    public WalletController(
            WalletService walletService,
            TransactionHistoryService transactionHistoryService,
            BalanceService balanceService) {
        this.walletService = walletService;
        this.transactionHistoryService = transactionHistoryService;
        this.balanceService = balanceService;
    }

    /**
     * Retrieves the wallet information for the authenticated user.
     *
     * @return ResponseEntity containing the WalletDTO with wallet data
     */
    @GetMapping
    public ResponseEntity<WalletDTO> getWallet() {
        Integer userId = getAuthenticatedUserId();
        return ResponseEntity.ok(
                new WalletDTO(walletService.getWalletByUserId(userId))
        );
    }

    /**
     * Returns the ledger-derived balance for the authenticated user's wallet.
     *
     * @return ResponseEntity containing available, staked, and total balance
     */
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance() {
        Integer userId = getAuthenticatedUserId();
        Wallet wallet = walletService.getWalletByUserId(userId);
        BalanceResponse fullBalance = balanceService.getBalance(wallet.getWalletAddress());
        BigDecimal spendable = balanceService.getSpendableBalance(wallet.getWalletAddress());

        return ResponseEntity.ok(new BalanceResponse(
                wallet.getWalletAddress(),
                spendable,
                fullBalance.getStaked(),
                spendable.add(fullBalance.getStaked())
        ));
    }

    /**
     * Returns paginated transaction history for the authenticated user's wallet.
     *
     * @param page optional 1-based page number
     * @param limit optional page size
     * @return paginated transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<TransactionHistoryResponseDTO> getWalletTransactions(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        Integer userId = getAuthenticatedUserId();
        return ResponseEntity.ok(
                transactionHistoryService.getUserTransactions(userId, page, limit)
        );
    }

    private Integer getAuthenticatedUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}