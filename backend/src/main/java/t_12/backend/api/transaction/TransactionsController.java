package t_12.backend.api.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.api.coin.PurchaseRequest;
import t_12.backend.api.coin.PurchaseResponse;
import t_12.backend.api.transaction.dto.TransactionHistoryResponseDTO;
import t_12.backend.entity.Transaction;
import t_12.backend.exception.ForbiddenException;
import t_12.backend.service.PurchaseService;
import t_12.backend.service.TransactionHistoryService;
import t_12.backend.service.TransactionService;
import t_12.backend.service.TransactionValidationService;

/**
 * Purchase history API, blockchain transfer submission, transaction
 * lookup, and a backward-compatible buy route for the marketplace page.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionsController {

    private final TransactionHistoryService transactionHistoryService;
    private final PurchaseService purchaseService;
    private final TransactionService transactionService;
    private final TransactionValidationService validationService;

    public TransactionsController(
            TransactionHistoryService transactionHistoryService,
            PurchaseService purchaseService,
            TransactionService transactionService,
            TransactionValidationService validationService) {
        this.transactionHistoryService = transactionHistoryService;
        this.purchaseService = purchaseService;
        this.transactionService = transactionService;
        this.validationService = validationService;
    }

    /**
     * Returns paginated buy and sell history for the resolved user.
     *
     * @param userId authenticated user id header
     * @param page   optional 1-based page number
     * @param limit  optional page size
     * @return paginated transaction history
     */
    @GetMapping
    public ResponseEntity<TransactionHistoryResponseDTO> getTransactions(
            @RequestHeader(value = "x-user-id", required = false) Integer userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        Integer resolvedUserId = resolveAuthenticatedUserId(userId);
        return ResponseEntity.ok(
                transactionHistoryService.getUserTransactions(resolvedUserId, page, limit)
        );
    }

    /**
     * Keeps the existing marketplace purchase route under the transactions API.
     *
     * @param request purchase request body
     * @param userId  authenticated user id header
     * @return created purchase response
     */
    @PostMapping("/buy")
    public ResponseEntity<PurchaseResponse> buyCoinViaTransactionsRoute(
            @RequestBody PurchaseRequest request,
            @RequestHeader(value = "x-user-id", required = false) Integer userId) {
        Integer authenticatedUserId = resolveAuthenticatedUserId(userId);
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
     * POST /api/transactions/transfer
     * Submits a blockchain transfer between two wallet addresses.
     * Validates sender has sufficient ledger-derived balance before
     * creating the transaction. Returns the new transaction with
     * status PENDING and a SHA-256 hash for tracking.
     *
     * @param request the transfer details (sender, receiver, amount, fee, nonce)
     * @return the created transaction with 201 status
     */
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> submitTransfer(@RequestBody TransferRequest request) {
        validationService.validateBalance(
                request.getSenderAddress(), request.getAmount(), request.getFee());

        Transaction tx = transactionService.createTransaction(
                request.getSenderAddress(),
                request.getReceiverAddress(),
                request.getAmount(),
                request.getFee(),
                request.getNonce());

        return ResponseEntity.status(201).body(tx);
    }

    /**
     * GET /api/transactions/{hash}
     * Look up a transaction by its SHA-256 hash.
     *
     * @param hash the transaction hash
     * @return the transaction entity
     */
    @GetMapping("/{hash}")
    public ResponseEntity<Transaction> getByHash(@PathVariable String hash) {
        return ResponseEntity.ok(transactionService.findByHash(hash));
    }

    /**
     * Resolves user ID from JWT-backed SecurityContext and validates any
     * optional x-user-id header when present.
     *
     * @param headerUserId optional user ID from legacy clients
     * @return authenticated user ID from the security context
     */
    private Integer resolveAuthenticatedUserId(Integer headerUserId) {
        Integer authenticatedUserId = (Integer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (headerUserId != null && !headerUserId.equals(authenticatedUserId)) {
            throw new ForbiddenException("Forbidden: userId does not match authenticated user");
        }

        return authenticatedUserId;
    }
}
