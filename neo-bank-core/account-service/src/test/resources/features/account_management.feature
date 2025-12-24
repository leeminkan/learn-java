Feature: Account Management
  As a bank administrator
  I want to manage user accounts
  So that I can maintain the bank's ledger integrity

  Background:
    Given the account database is empty

  # --- HAPPY PATHS ---

  Scenario: Successfully create a new account
    When I create an account for "Alice" with email "alice@nab.com" and balance 1000.00
    Then I should receive a 201 Created response
    And the response should contain an Account ID

  Scenario: Retrieve an existing account
    Given an account exists for "Bob" with email "bob@nab.com"
    When I retrieve the account by its ID
    Then I should receive a 200 OK response
    And the response name should be "Bob"
    And the response email should be "bob@nab.com"

  # --- SAD PATHS (Error Handling) ---

  Scenario: Fail to create account with duplicate email
    Given an account exists for "Charlie" with email "charlie@nab.com"
    When I create an account for "Charlie Clone" with email "charlie@nab.com" and balance 500.00
    Then I should receive a 409 Conflict response

  Scenario: Fail to retrieve non-existent account
    When I retrieve an account with ID 999999
    Then I should receive a 404 Not Found response

  Scenario: Fail to create account with negative balance
    When I create an account for "Evil User" with email "evil@nab.com" and balance -100.00
    Then I should receive a 400 Bad Request response