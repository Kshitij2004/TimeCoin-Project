package t_12.backend.api.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.api.coin.PurchaseRequest;
import t_12.backend.api.coin.PurchaseResponse;
import t_12.backend.api.transaction.dto.TransactionHistoryResponseDTO;
import t_12.backend.exception.ForbiddenException;
import t_12.backend.service.PurchaseService;
import t_12.backend.service.TransactionHistoryService;

/**
 * Purchase history API plus a backward-compatible buy route for the
 * marketplace page.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionsController {

    private final TransactionHistoryService transactionHistoryService;
    private final PurchaseService purchaseService;

    /**
     * Creates the transaction API controller.
     *
     * @param transactionHistoryService service used to fetch paginated history
     * @param purchaseService service used for backward-compatible buy requests
     */
    public TransactionsController(
            TransactionHistoryService transactionHistoryService,
            PurchaseService purchaseService) {
        this.transactionHistoryService = transactionHistoryService;
        this.purchaseService = purchaseService;
    }

    /**
     * Returns paginated buy and sell history for the resolved user.
     *
     * @param userId authenticated user id header
     * @param page optional 1-based page number
     * @param limit optional page size
     * @return paginated transaction history
     */
    @GetMapping
    public ResponseEntity<TransactionHistoryResponseDTO> getTransactions(
            @RequestHeader(value = "x-user-id", required = false) Integer userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        Integer authenticatedUserId = getAuthenticatedUserId();
        if (userId != null && !userId.equals(authenticatedUserId)) {
            throw new ForbiddenException("Forbidden: x-user-id does not match authenticated user");
        }

        return ResponseEntity.ok(
                transactionHistoryService.getUserTransactions(authenticatedUserId, page, limit)
        );
    }

    /**
     * Keeps the existing marketplace purchase route under the transactions API.
     *
     * @param request purchase request body
     * @param userId authenticated user id header
     * @return created purchase response
     */
    @PostMapping("/buy")
    public ResponseEntity<PurchaseResponse> buyCoinViaTransactionsRoute(
            @RequestBody PurchaseRequest request,
            @RequestHeader(value = "x-user-id", required = false) Integer userId) {
        Integer authenticatedUserId = getAuthenticatedUserId();
        if (userId != null && !userId.equals(authenticatedUserId)) {
            throw new ForbiddenException("Forbidden: x-user-id does not match authenticated user");
        }
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
