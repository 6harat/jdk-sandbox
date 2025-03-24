package io.pop.saga;

public record OrderEvent(String id, OrderStatus status, double amount) {}
