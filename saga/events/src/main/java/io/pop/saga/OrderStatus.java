package io.pop.saga;

public enum OrderStatus {
    CREATED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    STOCK_RESERVED,
    STOCK_FAILED,
    COMPLETED,
    CANCELED,
    NOT_FOUND
}
