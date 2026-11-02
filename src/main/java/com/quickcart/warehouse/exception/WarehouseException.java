package com.quickcart.warehouse.exception;

/** Business / data-access failure surfaced to the CLI with a user-friendly message. */
public class WarehouseException extends RuntimeException {
    public WarehouseException(String message) {
        super(message);
    }

    public WarehouseException(String message, Throwable cause) {
        super(message, cause);
    }
}
