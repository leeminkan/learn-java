package org.leeminkan.bookstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Entity
@Data
@Table(name = "app_user") // Use a specific table name to avoid conflicts with SQL keywords
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String username; // Must be unique!

    @NotBlank
    private String password; // Will store the hashed value!

    // Simple string role for authorization checks (e.g., "USER" or "ADMIN")
    private String role;
}