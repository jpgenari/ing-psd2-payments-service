package com.ing.psd2.payments.steps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.web.server.LocalServerPort;

public class PaymentSteps {

    @LocalServerPort
    private int port;

    private String authHeader;
    private String xRequestId;
    private Map<String, Object> payload;
    private Response response;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        payload = new HashMap<>();
        xRequestId = null;
        authHeader = null;
    }

    @Given("the PSD2 payment initiation endpoint is available")
    public void thePsd2PaymentInitiationEndpointIsAvailable() {
        // Spring Boot embedded server is booted via CucumberSpringConfiguration
    }

    @And("I have a valid PSD2 OAuth2 bearer token")
    public void iHaveAValidPsd2Oauth2BearerToken() {
        this.authHeader = "Bearer valid-psd2-pis-token-2026";
    }

    @Given("I have an expired PSD2 OAuth2 bearer token")
    public void iHaveAnExpiredPsd2Oauth2BearerToken() {
        this.authHeader = "Bearer expired-psd2-pis-token";
    }

    @Given("I have a read-only PSD2 OAuth2 bearer token")
    public void iHaveAReadOnlyPsd2Oauth2BearerToken() {
        this.authHeader = "Bearer readonly-ais-token";
    }

    @Given("a unique {string} header is generated")
    public void aUniqueHeaderIsGenerated(String headerName) {
        if ("X-Request-ID".equalsIgnoreCase(headerName)) {
            this.xRequestId = UUID.randomUUID().toString();
        }
    }

    @Given("the {string} header is omitted")
    public void theHeaderIsOmitted(String headerName) {
        if ("X-Request-ID".equalsIgnoreCase(headerName)) {
            this.xRequestId = null;
        }
    }

    @Given("the {string} header is set to {string}")
    public void theHeaderIsSetTo(String headerName, String value) {
        if ("X-Request-ID".equalsIgnoreCase(headerName)) {
            this.xRequestId = value;
        }
    }

    @And("a valid payment payload with amount {double} and currency {string}")
    public void aValidPaymentPayloadWithAmountAndCurrency(double amount, String currency) {
        payload.put("debtorIban", "NL91ABNA0417164300");
        payload.put("creditorIban", "DE89370400440532013000");
        payload.put("creditorName", "Acme Cloud GmbH");
        payload.put("amount", BigDecimal.valueOf(amount));
        payload.put("currency", currency);
        payload.put("remittanceInformation", "SEPA PSD2 Demo Transfer");
    }

    @When("I send a POST request to {string}")
    public void iSendAPostRequestTo(String endpoint) {
        RequestSpecification request = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload);

        if (authHeader != null) {
            request.header("Authorization", authHeader);
        }
        if (xRequestId != null) {
            request.header("X-Request-ID", xRequestId);
        }

        this.response = request.when().post(endpoint);
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {
        response.then().statusCode(expectedStatusCode);
    }

    @And("the response JSON field {string} should equal {string}")
    public void theResponseJsonFieldShouldEqual(String jsonPath, String expectedValue) {
        response.then().body(jsonPath, equalTo(expectedValue));
    }

    @And("the response JSON field {string} should not be empty")
    public void theResponseJsonFieldShouldNotBeEmpty(String jsonPath) {
        response.then().body(jsonPath, not(emptyOrNullString()));
    }
}
