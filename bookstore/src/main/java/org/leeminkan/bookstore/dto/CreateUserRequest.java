package org.leeminkan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username is required and cannot be empty.")
    private String username;

    @NotBlank(message = "Password is required and cannot be empty.")
    private String password;
}