# ING PSD2 Payments Service

A small Spring Boot 3 service exposing a Berlin Group NextGenPSD2-style **single payment initiation** endpoint, together with a headless **Cucumber (BDD) + REST Assured** test suite. It is built as a "Zero UI" demo: everything is backend APIs, JSON contracts and validation rules, with no browser involved.

## Tech stack

- Java 17, Spring Boot 3.3.4 (Web, Validation)
- Cucumber 7.18.1 (JUnit Platform engine) with `cucumber-spring`
- REST Assured for HTTP assertions
- BrowserStack Java SDK (reports runs to Test Observability)
- Maven

## Repository layout

```
ing-psd2-payments-service/
├── browserstack.yml             # BrowserStack SDK config (credentials via env vars)
├── openapi.yaml                 # API contract (source of truth)
├── pom.xml
└── src/
    ├── main/java/com/ing/psd2/payments/
    │   ├── PaymentsApplication.java
    │   ├── controller/PaymentController.java
    │   └── model/               # PaymentRequest, PaymentResponse, ErrorResponse
    └── test/
        ├── java/com/ing/psd2/payments/
        │   ├── RunCucumberTest.java              # JUnit Platform suite entry point
        │   └── steps/
        │       ├── CucumberSpringConfiguration.java  # Boots the app on a random port
        │       └── PaymentSteps.java                 # REST Assured step definitions
        └── resources/features/payment_psd2.feature   # Gherkin scenarios
```

## The API

`POST /v1/payments`

| Header | Required | Notes |
|---|---|---|
| `Authorization` | yes | `Bearer <token>`. A token containing `expired` returns 401; one containing `readonly` returns 403 (demo rules). |
| `X-Request-ID` | yes | Must be a UUID; also used for idempotency (a repeated ID returns 409). |

Body (`PaymentRequest`): `debtorIban`, `creditorIban`, `creditorName`, `amount` (>= 0.01), `currency` (`EUR|GBP|USD|CHF`), optional `remittanceInformation` (max 140 chars).

| Status | `error` | When |
|---|---|---|
| 201 | n/a (`transactionStatus: ACCP`) | Valid payment |
| 400 | `MISSING_HEADER` | `X-Request-ID` absent |
| 400 | `INVALID_HEADER` | `X-Request-ID` not a UUID |
| 400 | `VALIDATION_ERROR` | Bean validation failure (bad currency, amount <= 0, ...) |
| 401 | `UNAUTHORIZED` | Missing or expired bearer token |
| 403 | `INSUFFICIENT_SCOPE` | Read-only token |
| 409 | `DUPLICATE_REQUEST` | Reused `X-Request-ID` |

The full contract is in `openapi.yaml`.

## The tests

Feature file: `src/test/resources/features/payment_psd2.feature` (tagged `@PSD2`). Each scenario starts the real Spring Boot app on a random port (via `CucumberSpringConfiguration`) and calls it over HTTP with REST Assured, so it is fast and needs no browser.

| Scenario | Tags | Expected |
|---|---|---|
| Successfully initiate a valid SEPA payment | `@happy-path` | 201, `transactionStatus = ACCP`, `paymentId` present |
| Missing `X-Request-ID` header | `@validation @negative` | 400 `MISSING_HEADER` |
| `X-Request-ID` is not a UUID | `@validation @boundary` | 400 `INVALID_HEADER` |
| Unsupported currency code (`XYZ`) | `@validation @boundary` | 400 `VALIDATION_ERROR` |
| Zero or negative amount (outline: `0.00`, `-50.00`) | `@validation @boundary` | 400 `VALIDATION_ERROR` |
| Expired bearer token | `@security` | 401 `UNAUTHORIZED` |
| Read-only bearer token | `@security` | 403 `INSUFFICIENT_SCOPE` |

That is 7 scenarios and 8 executed tests (the outline runs twice).

Step definitions live in `PaymentSteps.java`; status codes and JSON fields are asserted with REST Assured/Hamcrest.

## Running the tests

```bash
mvn test-compile
mvn test -Dtest=RunCucumberTest -Dcucumber.filter.tags="@PSD2"
```

Run a subset by tag, e.g. `-Dcucumber.filter.tags="@security"` or `"@PSD2 and @boundary"`.

## BrowserStack reporting

`browserstack.yml` enables the BrowserStack Java SDK (attached as a `-javaagent` on surefire in `pom.xml`) with Test Observability. Credentials are read from the environment, never stored in files:

```bash
export BROWSERSTACK_USERNAME=<your username>
export BROWSERSTACK_ACCESS_KEY=<your access key>
mvn test -Dtest=RunCucumberTest -Dcucumber.filter.tags="@PSD2"
```

The console prints a link to the build on `automation.browserstack.com` at the end of the run. The tests themselves still execute locally against the embedded server; BrowserStack receives the results.

## Demo: breaking the contract

In `PaymentController.java`, find the `// DEMO STEP 5` comment and change `HttpStatus.BAD_REQUEST` to `HttpStatus.UNPROCESSABLE_ENTITY` for the missing-header response. Re-run the suite: the "Missing X-Request-ID" scenario fails (expected 400, got 422), which is a good starting point for failure triage with Test Companion.
