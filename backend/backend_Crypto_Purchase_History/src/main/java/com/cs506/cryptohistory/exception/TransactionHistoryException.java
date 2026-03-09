package com.cs506.cryptohistory.exception;

public class TransactionHistoryException extends RuntimeException {
    private final int statusCode;

    public TransactionHistoryException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
