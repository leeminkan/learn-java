package org.leeminkan.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private LocalDateTime createdAt;
}