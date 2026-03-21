package t_12.backend.api.coin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.exception.ForbiddenException;
import t_12.backend.service.CoinService;
import t_12.backend.service.PurchaseService;

/**
 * Controller for handling coin-related HTTP requests.
 */
@RestController
@RequestMapping({"/api/coin", "/api/coins"})
public class CoinController {

    private final CoinService coinService;
    private final PurchaseService purchaseService;

    public CoinController(CoinService coinService, PurchaseService purchaseService) {
        this.coinService = coinService;
        this.purchaseService = purchaseService;
    }

    /**
     * Retrieves the current coin information.
     *
     * @return ResponseEntity containing the CoinDTO with current coin data
     */
    @GetMapping
    public ResponseEntity<CoinDTO> getCoin() {
        return ResponseEntity.ok(new CoinDTO(coinService.getCurrentCoin()));
    }

    /**
     * Purchases TimeCoin for the requested user.
     *
     * @param request the purchase request body
     * @return the saved transaction and updated wallet state
     */
    @PostMapping("/buy")
    public ResponseEntity<PurchaseResponse> buyCoin(
            @RequestBody PurchaseRequest request) {
        Integer authenticatedUserId = getAuthenticatedUserId();
        if (request.getUserId() != null && !request.getUserId().equals(authenticatedUserId)) {
            throw new ForbiddenException("Forbidden: userId does not match authenticated user");
        }

        return ResponseEntity.status(201).body(
                purchaseService.purchaseCoin(
                        authenticatedUserId,
                        request.getSymbol(),
                        request.getAmount()
                )
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
