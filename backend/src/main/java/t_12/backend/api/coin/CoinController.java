package t_12.backend.api.coin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.exception.ForbiddenException;
import t_12.backend.service.CoinService;
import t_12.backend.service.PriceHistoryService;
import t_12.backend.service.PurchaseService;

/**
 * Controller for handling coin-related HTTP requests.
 */
@RestController
@RequestMapping({"/api/coin", "/api/coins"})
public class CoinController {

    private final CoinService coinService;
    private final PurchaseService purchaseService;
    private final PriceHistoryService priceHistoryService;

    public CoinController(CoinService coinService,
                          PurchaseService purchaseService,
                          PriceHistoryService priceHistoryService) {
        this.coinService = coinService;
        this.purchaseService = purchaseService;
        this.priceHistoryService = priceHistoryService;
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
     * Returns chronological price history data points for charting.
     *
     * Optional query param "range" filters by time window:
     *   - "1h"  = last hour
     *   - "24h" = last 24 hours (default)
     *   - "7d"  = last 7 days
     *   - "30d" = last 30 days
     *   - "all" = all recorded history
     *
     * @param range optional time range filter
     * @return list of price data points in chronological order
     */
    @GetMapping("/price-history")
    public ResponseEntity<List<PriceHistoryDTO>> getPriceHistory(
            @RequestParam(required = false, defaultValue = "24h") String range) {

        List<PriceHistoryDTO> history;

        if ("all".equalsIgnoreCase(range)) {
            history = priceHistoryService.getAll().stream()
                    .map(PriceHistoryDTO::new)
                    .toList();
        } else {
            LocalDateTime since = parseSince(range);
            history = priceHistoryService.getSince(since).stream()
                    .map(PriceHistoryDTO::new)
                    .toList();
        }

        return ResponseEntity.ok(history);
    }

    /**
     * Parses a range string into a LocalDateTime cutoff.
     */
    private LocalDateTime parseSince(String range) {
        LocalDateTime now = LocalDateTime.now();
        return switch (range.toLowerCase()) {
            case "1h" -> now.minusHours(1);
            case "24h" -> now.minusHours(24);
            case "7d" -> now.minusDays(7);
            case "30d" -> now.minusDays(30);
            default -> now.minusHours(24);
        };
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
     * Sells TimeCoin for the authenticated user.
     *
     * @param request the sell request body (same shape as buy)
     * @return the saved transaction and updated wallet state
     */
    @PostMapping("/sell")
    public ResponseEntity<PurchaseResponse> sellCoin(
            @RequestBody PurchaseRequest request) {
        Integer authenticatedUserId = getAuthenticatedUserId();
        if (request.getUserId() != null && !request.getUserId().equals(authenticatedUserId)) {
            throw new ForbiddenException("Forbidden: userId does not match authenticated user");
        }

        return ResponseEntity.status(201).body(
                purchaseService.sellCoin(
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
