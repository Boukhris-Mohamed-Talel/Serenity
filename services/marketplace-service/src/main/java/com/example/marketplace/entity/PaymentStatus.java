package com.example.marketplace.entity;

public enum PaymentStatus {
    /** Checkout created; no payment gateway — awaiting staff confirmation. */
    PENDING,
    /** Admin confirmed / fulfilled without online payment. */
    NOT_REQUIRED,
    MOCK_AUTHORIZED,
    MOCK_DECLINED
}
