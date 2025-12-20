package org.leeminkan.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBookRequest {

    @NotBlank(message = "Book title is required and cannot be empty.")
    private String title;

    // Assuming we added 'author' earlier for realism
    @NotBlank(message = "Author is required.")
    private String author;
}