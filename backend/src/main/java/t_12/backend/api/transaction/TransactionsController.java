package t_12.backend.api.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import t_12.backend.api.coin.PurchaseRequest;
import t_12.backend.api.coin.PurchaseResponse;
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

    public TransactionsController(
            TransactionHistoryService transactionHistoryService,
            PurchaseService purchaseService) {
        this.transactionHistoryService = transactionHistoryService;
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public ResponseEntity<TransactionHistoryResponse> getTransactions(
            @RequestHeader(value = "x-user-id", required = false) Integer userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(
                transactionHistoryService.getUserTransactions(userId, page, limit)
        );
    }

    @PostMapping("/buy")
    public ResponseEntity<PurchaseResponse> buyCoinViaTransactionsRoute(
            @RequestBody PurchaseRequest request,
            @RequestHeader(value = "x-user-id", required = false) Integer userId) {
        Integer resolvedUserId = request.getUserId() == null ? userId : request.getUserId();
        return ResponseEntity.status(201).body(
                purchaseService.purchaseCoin(
                        resolvedUserId,
                        request.getSymbol(),
                        request.getAmount()
                )
        );
    }
}
