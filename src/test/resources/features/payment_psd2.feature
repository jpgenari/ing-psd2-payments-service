@PSD2
Feature: PSD2 Single Payment Initiation Validation
  As a PSD2 Payment Initiation Service Provider (PISP)
  I want to initiate SEPA credit transfers via /v1/payments
  So that payments are validated against Berlin Group PSD2 compliance rules

  Background:
    Given the PSD2 payment initiation endpoint is available
    And I have a valid PSD2 OAuth2 bearer token

  @happy-path
  Scenario: Successfully initiate a valid SEPA payment
    Given a unique "X-Request-ID" header is generated
    And a valid payment payload with amount 250.00 and currency "EUR"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 201
    And the response JSON field "transactionStatus" should equal "ACCP"
    And the response JSON field "paymentId" should not be empty

  @validation @negative
  Scenario: Reject payment when mandatory X-Request-ID header is missing
    Given the "X-Request-ID" header is omitted
    And a valid payment payload with amount 120.50 and currency "EUR"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 400
    And the response JSON field "error" should equal "MISSING_HEADER"

  @validation @boundary
  Scenario: Reject payment when X-Request-ID is not a valid UUID
    Given the "X-Request-ID" header is set to "not-a-valid-uuid"
    And a valid payment payload with amount 120.50 and currency "EUR"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 400
    And the response JSON field "error" should equal "INVALID_HEADER"

  @validation @boundary
  Scenario: Reject payment with unsupported ISO currency code
    Given a unique "X-Request-ID" header is generated
    And a valid payment payload with amount 75.00 and currency "XYZ"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 400
    And the response JSON field "error" should equal "VALIDATION_ERROR"

  @validation @boundary
  Scenario Outline: Reject payment with zero or negative amount
    Given a unique "X-Request-ID" header is generated
    And a valid payment payload with amount <amount> and currency "EUR"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 400
    And the response JSON field "error" should equal "VALIDATION_ERROR"

    Examples:
      | amount |
      | 0.00   |
      | -50.00 |

  @security
  Scenario: Reject payment when OAuth2 bearer token is expired
    Given I have an expired PSD2 OAuth2 bearer token
    And a unique "X-Request-ID" header is generated
    And a valid payment payload with amount 100.00 and currency "EUR"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 401
    And the response JSON field "error" should equal "UNAUTHORIZED"

  @security
  Scenario: Reject payment when OAuth2 token lacks PIS write permissions
    Given I have a read-only PSD2 OAuth2 bearer token
    And a unique "X-Request-ID" header is generated
    And a valid payment payload with amount 100.00 and currency "EUR"
    When I send a POST request to "/v1/payments"
    Then the response status code should be 403
    And the response JSON field "error" should equal "INSUFFICIENT_SCOPE"
