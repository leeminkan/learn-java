package org.leeminkan.account.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.leeminkan.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class AccountSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    private Response lastResponse;
    private Long lastCreatedAccountId; // To store ID between steps

    @Given("the account database is empty")
    public void databaseIsEmpty() {
        accountRepository.deleteAll();
    }

    // --- REUSABLE HELPER STEP ---
    @Given("an account exists for {string} with email {string}")
    public void createPreExistingAccount(String name, String email) {
        // Reuse the creation logic to set up state
        createAccount(name, email, 100.00);

        // Ensure it was created successfully
        lastResponse.then().statusCode(201);

        // Save the ID for later use
        lastCreatedAccountId = lastResponse.jsonPath().getLong("id");
    }

    @When("I create an account for {string} with email {string} and balance {double}")
    public void createAccount(String name, String email, Double balance) {
        Map<String, Object> body = new HashMap<>();
        body.put("holderName", name);
        body.put("email", email);
        body.put("initialBalance", balance);

        lastResponse = given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/accounts");
    }

    @When("I retrieve the account by its ID")
    public void retrieveAccountById() {
        lastResponse = given()
                .port(port)
                .get("/api/accounts/" + lastCreatedAccountId);
    }

    @When("I retrieve an account with ID {long}")
    public void retrieveAccountBySpecificId(Long id) {
        lastResponse = given()
                .port(port)
                .get("/api/accounts/" + id);
    }

    // --- ASSERTIONS ---

    @Then("I should receive a {int} {word} response")
    public void verifyStatusCode(int statusCode, String statusText) {
        // We ignore the statusText (e.g., "Created", "OK") in the code,
        // it's just for readability in the feature file.
        lastResponse.then().statusCode(statusCode);
    }

    @Then("the response should contain an Account ID")
    public void verifyId() {
        lastResponse.then().body("id", notNullValue());
    }

    @Then("the response name should be {string}")
    public void verifyName(String expectedName) {
        lastResponse.then().body("holderName", equalTo(expectedName));
    }

    @Then("the response email should be {string}")
    public void verifyEmail(String expectedEmail) {
        lastResponse.then().body("email", equalTo(expectedEmail));
    }

    // Add this step for "Not Found"
    @Then("I should receive a {int} Not Found response")
    public void verifyNotFound(int statusCode) {
        lastResponse.then().statusCode(statusCode);
    }

    // Add this step for "Bad Request" and "Conflict"
    // Using {string} to match "Bad Request" or "Conflict" generically
    @Then("I should receive a {int} {string} response")
    public void verifyGenericStatus(int statusCode, String ignoredText) {
        lastResponse.then().statusCode(statusCode);
    }

    @Then("I should receive a {int} Bad Request response")
    public void verifyBadRequest(int statusCode) {
        lastResponse.then().statusCode(statusCode);
    }
}