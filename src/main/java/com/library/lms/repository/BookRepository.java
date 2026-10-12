package com.library.lms.repository;

import com.library.lms.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Book entity operations.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Find a book by its unique ISBN.
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Check if a book already exists with the given ISBN.
     */
    boolean existsByIsbn(String isbn);

    /**
     * Search books matching title or author (case-insensitive substring search) with pagination.
     */
    Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author, Pageable pageable);

    /**
     * Filter books by category.
     */
    List<Book> findByCategoryIgnoreCase(String category);

    /**
     * Pessimistic Write Lock: Acquires a row-level database lock (SELECT ... FOR UPDATE)
     * to prevent race conditions and over-issuing when concurrent transactions modify copy counts.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);
}
