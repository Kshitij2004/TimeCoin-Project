package com.cs506.cryptopurchase.exception;

public class PurchaseException extends RuntimeException {
    private final int statusCode;

    public PurchaseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
