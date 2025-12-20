package org.leeminkan.bookstore.repository;

import org.leeminkan.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Marks this as a component for the Spring container
public interface BookRepository extends JpaRepository<Book, Long> {
    // Spring Data JPA automatically provides:
    // - save(), findAll(), findById(), delete(), etc.
    // We don't need to write any implementation code!

    // Spring generates: SELECT * FROM book WHERE title LIKE %keyword% (case insensitive)
    List<Book> findByTitleContainingIgnoreCase(String keyword);
}