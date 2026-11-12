package com.ing.psd2.payments.model;

public record ErrorResponse(
    String error,
    String message
) {}
