package com.ing.psd2.payments.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentRequest(
    @NotBlank(message = "debtorIban must not be blank")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", message = "Invalid debtor IBAN format")
    String debtorIban,

    @NotBlank(message = "creditorIban must not be blank")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", message = "Invalid creditor IBAN format")
    String creditorIban,

    @NotBlank(message = "creditorName must not be blank")
    String creditorName,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^(EUR|GBP|USD|CHF)$", message = "Unsupported ISO 4217 currency code")
    String currency,

    @Size(max = 140, message = "remittanceInformation cannot exceed 140 characters")
    String remittanceInformation
) {}
