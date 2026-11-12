package com.ing.psd2.payments.controller;

import com.ing.psd2.payments.model.ErrorResponse;
import com.ing.psd2.payments.model.PaymentRequest;
import com.ing.psd2.payments.model.PaymentResponse;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private static final String UUID_REGEX =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final Set<String> processedRequestIds = ConcurrentHashMap.newKeySet();

    @PostMapping
    public ResponseEntity<?> initiatePayment(
        @RequestHeader(value = "X-Request-ID", required = false) String xRequestId,
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody PaymentRequest request
    ) {
        // 1. OAuth2 / PSD2 Scope Validation
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.contains("expired")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Valid PSD2 Bearer token is required"));
        }
        if (authorization.contains("readonly")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("INSUFFICIENT_SCOPE", "Token lacks PIS payment initiation scope"));
        }

        // 2. Mandatory PSD2 X-Request-ID Header Validation
        if (xRequestId == null || xRequestId.isBlank()) {
            // DEMO STEP 5: Change HttpStatus.BAD_REQUEST to HttpStatus.UNPROCESSABLE_ENTITY to simulate contract regression
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MISSING_HEADER", "Mandatory PSD2 header X-Request-ID is missing"));
        }

        if (!xRequestId.matches(UUID_REGEX)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_HEADER", "X-Request-ID must be a valid UUIDv4"));
        }

        // 3. Idempotency Check
        if (!processedRequestIds.add(xRequestId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_REQUEST", "Duplicate X-Request-ID detected"));
        }

        PaymentResponse response = new PaymentResponse(
            "PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "ACCP",
            xRequestId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("Invalid payment request payload");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", detail));
    }
}
