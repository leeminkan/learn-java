package org.leeminkan.bookstore.service;

import org.leeminkan.bookstore.dto.CreateBookRequest;
import org.leeminkan.bookstore.mapper.BookMapper;
import org.leeminkan.bookstore.model.Book;
import org.leeminkan.bookstore.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper; // Inject the Mapper Interface

    // This is Constructor Injection. @Autowired is optional in recent Spring Boot,
    // but the presence of the constructor signals the dependency.
    // Constructor Injection updated to include the mapper
    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    // Example of a Service Method
    public List<Book> getAllBooks() {
        // Business logic would go here before calling the repository (if any)
        return bookRepository.findAll();
    }

    // Update the method signature to accept the DTO
    public Book save(CreateBookRequest request) {
        // Use MapStruct to convert the DTO to an Entity
        Book newBook = bookMapper.toEntity(request);

        // Manual work is now only for fields NOT in the DTO (like generated dates)
        newBook.setCreatedAt(LocalDateTime.now());

        return bookRepository.save(newBook);
    }

    public Optional<Book> findById(Long id) {
        // Optional is a container object used to contain not-null values.
        // It helps handle the case where the entity might not exist.
        return bookRepository.findById(id);
    }

    public Optional<Book> update(Long id, Book bookDetails) {
        return bookRepository.findById(id)
                .map(existingBook -> {
                    // Step 2: Update fields of the existing entity
                    existingBook.setTitle(bookDetails.getTitle());
                    existingBook.setUpdatedAt(LocalDateTime.now());

                    // Step 3: Save the updated entity
                    return bookRepository.save(existingBook);
                });
        // If findById(id) returns empty, .map() is skipped and the empty Optional is returned.
    }

    public boolean delete(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Book> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCase(keyword);
    }
}