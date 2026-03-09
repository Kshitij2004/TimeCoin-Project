package com.cs506.cryptohistory.controller;

import com.cs506.cryptohistory.dto.ApiError;
import com.cs506.cryptohistory.dto.TransactionHistoryResponse;
import com.cs506.cryptohistory.exception.TransactionHistoryException;
import com.cs506.cryptohistory.service.TransactionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
public class TransactionsController {

    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public ResponseEntity<TransactionHistoryResponse> getTransactions(
        @RequestHeader(value = "x-user-id", required = false) Integer userId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(transactionsService.getUserTransactions(userId, page, limit));
    }

    @ExceptionHandler(TransactionHistoryException.class)
    public ResponseEntity<ApiError> handleDomainError(TransactionHistoryException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return ResponseEntity.status(500).body(new ApiError("Failed to fetch transactions"));
    }
}
