package com.library.lms.controller;

import com.library.lms.dto.request.BookRequestDTO;
import com.library.lms.dto.response.BookResponseDTO;
import com.library.lms.dto.response.PagedResponse;
import com.library.lms.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing book catalog operations.
 * - GET endpoints: open to all members & guests.
 * - POST, PUT, DELETE: strictly protected with @PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * List all books with pagination and sorting.
     * Public access.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<BookResponseDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PagedResponse<BookResponseDTO> response = bookService.getAllBooks(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Get details of a single book by ID.
     * Public access.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> getBookById(@PathVariable Long id) {
        BookResponseDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    /**
     * Search books by title or author with pagination.
     * Public access.
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<BookResponseDTO>> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PagedResponse<BookResponseDTO> response = bookService.searchBooks(query, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a new book to the library catalog.
     * Restricted strictly to ADMIN role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponseDTO> createBook(@Valid @RequestBody BookRequestDTO request) {
        BookResponseDTO created = bookService.createBook(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing book by ID.
     * Restricted strictly to ADMIN role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponseDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequestDTO request
    ) {
        BookResponseDTO updated = bookService.updateBook(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a book by ID.
     * Restricted strictly to ADMIN role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
