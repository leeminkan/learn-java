package org.leeminkan.bookstore.controller;

import jakarta.validation.Valid;
import org.leeminkan.bookstore.dto.BookResponse;
import org.leeminkan.bookstore.dto.CreateBookRequest;
import org.leeminkan.bookstore.mapper.BookMapper;
import org.leeminkan.bookstore.model.Book;
import org.leeminkan.bookstore.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/books") // Base path for all methods in this controller
public class BookController {

    private final BookService bookService;
    private final BookMapper bookMapper; // Inject the Mapper

    // Update the constructor to inject the mapper
    public BookController(BookService bookService, BookMapper bookMapper) {
        this.bookService = bookService;
        this.bookMapper = bookMapper;
    }

    // Maps to GET /api/books
    @GetMapping
    public List<BookResponse> getAllBooks() {
        // 1. Service returns raw Entity list
        List<Book> bookEntities = bookService.getAllBooks();

        // 2. Controller converts Entities to DTOs for the external response
        return bookMapper.toResponseList(bookEntities);
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody CreateBookRequest request) {
        // If validation fails, Spring throws an exception BEFORE this line executes.
        Book savedBook = bookService.save(request);

        // ResponseEntity allows us to explicitly set the HTTP status code
        return new ResponseEntity<>(savedBook, HttpStatus.CREATED); // Returns 201 Created
    }

    // Maps to GET /api/books/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Optional<Book> book = bookService.findById(id);

        // Check if the Optional contains a value:
        // Returns the Book object with 200 OK
        // Returns an empty response with 404 Not Found
        return book.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Maps to PUT /api/books/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long id,
            @RequestBody Book bookDetails) {

        // Call the service to perform the update
        Optional<Book> updatedBook = bookService.update(id, bookDetails);

        // Returns the updated Book object with 200 OK
        // Returns 404 Not Found if the ID was not in the database
        return updatedBook.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Maps to DELETE /api/books/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        boolean deleted = bookService.delete(id);

        if (deleted) {
            // Returns 204 No Content
            return ResponseEntity.noContent().build();
        } else {
            // Returns 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    // Maps to GET /api/books/search?keyword=...
    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // If no keyword is provided, return all books
            return bookService.getAllBooks();
        }
        // Otherwise, call our new service method
        return bookService.searchBooks(keyword);
    }
}