package com.cs506.cryptopurchase.controller;

import com.cs506.cryptopurchase.dto.ApiError;
import com.cs506.cryptopurchase.dto.PurchaseRequest;
import com.cs506.cryptopurchase.dto.PurchaseResponse;
import com.cs506.cryptopurchase.exception.PurchaseException;
import com.cs506.cryptopurchase.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coin")
public class CoinController {

    private final PurchaseService purchaseService;

    public CoinController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/buy")
    public ResponseEntity<PurchaseResponse> buyCoin(@RequestBody PurchaseRequest request) {
        PurchaseResponse response = purchaseService.purchaseCoin(
            request.userId(),
            request.symbol(),
            request.amount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(PurchaseException.class)
    public ResponseEntity<ApiError> handlePurchaseException(PurchaseException exception) {
        return ResponseEntity
            .status(exception.getStatusCode())
            .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError("Failed to purchase coin"));
    }
}
