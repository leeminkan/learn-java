package org.leeminkan.bookstore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data; // From the Lombok dependency

@Entity
@Data // Lombok automatically creates getters, setters, toString(), etc.
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Use Long for auto-incrementing ID

    @NotBlank(message = "Book title is required and cannot be empty.")
    private String title;

    // We will typically use Spring Auditing for these, but defining them
    // as fields is the first step.
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    // We can add a simple property like 'private String author;' later!
    private String author;
}