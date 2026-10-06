package com.example.demo.exception;

public final class ShopException extends RuntimeException {
    private final int status;
    public ShopException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
}
