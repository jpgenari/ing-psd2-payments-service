package com.ing.psd2.payments.model;

public record PaymentResponse(
    String paymentId,
    String transactionStatus,
    String xRequestId
) {}
