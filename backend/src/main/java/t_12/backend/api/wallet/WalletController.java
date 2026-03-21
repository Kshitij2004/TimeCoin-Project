package t_12.backend.api.wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.exception.ForbiddenException;
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
     * Retrieves wallet information for the authenticated user. The optional
     * userId query parameter is accepted only when it matches the JWT user.
     *
     * @param userId optional user ID to validate against the authenticated user
     * @return ResponseEntity containing the WalletDTO with wallet data
     */
    @GetMapping
    public ResponseEntity<WalletDTO> getWallet(
            @RequestParam(required = false) Integer userId) {
        Integer authenticatedUserId = getAuthenticatedUserId();
        if (userId != null && !userId.equals(authenticatedUserId)) {
            throw new ForbiddenException("Forbidden: userId does not match authenticated user");
        }

        return ResponseEntity.ok(
                new WalletDTO(walletService.getWalletByUserId(authenticatedUserId))
        );
    }

    /**
     * Reads the authenticated user ID from the SecurityContext.
     *
     * @return authenticated user ID from JWT principal
     */
    private Integer getAuthenticatedUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
