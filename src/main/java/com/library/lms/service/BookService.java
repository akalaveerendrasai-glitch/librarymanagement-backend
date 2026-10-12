package com.library.lms.service;

import com.library.lms.dto.request.BookRequestDTO;
import com.library.lms.dto.response.BookResponseDTO;
import com.library.lms.dto.response.PagedResponse;
import com.library.lms.entity.Book;
import com.library.lms.entity.Borrowing;
import com.library.lms.entity.BorrowingStatus;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    public BookService(BookRepository bookRepository, BorrowingRepository borrowingRepository) {
        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
    }

    /**
     * Retrieve all books with pagination and sorting.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BookResponseDTO> getAllBooks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Book> booksPage = bookRepository.findAll(pageable);

        return mapToPagedResponse(booksPage);
    }

    /**
     * Retrieve a specific book by ID.
     */
    @Transactional(readOnly = true)
    public BookResponseDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
        return BookResponseDTO.fromEntity(book);
    }

    /**
     * Search books by title or author with pagination and sorting.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BookResponseDTO> searchBooks(String query, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Book> booksPage = bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query, pageable);

        return mapToPagedResponse(booksPage);
    }

    /**
     * Add a new book to the library catalog.
     */
    @Transactional
    public BookResponseDTO createBook(BookRequestDTO request) {
        // 1. Check for duplicate ISBN
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException("A book with ISBN '" + request.getIsbn() + "' already exists");
        }

        // 2. Validate copy counts
        int total = request.getTotalCopies();
        int available = request.getAvailableCopies() != null ? request.getAvailableCopies() : total;

        if (available > total) {
            throw new BadRequestException("Available copies (" + available + ") cannot exceed total copies (" + total + ")");
        }

        // 3. Map to Entity and save
        Book book = new Book();
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(request.getIsbn().trim());
        book.setCategory(request.getCategory().trim());
        book.setTotalCopies(total);
        book.setAvailableCopies(available);
        book.setDescription(request.getDescription());

        Book savedBook = bookRepository.save(book);
        return BookResponseDTO.fromEntity(savedBook);
    }

    /**
     * Update an existing book's details and copy counts.
     */
    @Transactional
    public BookResponseDTO updateBook(Long id, BookRequestDTO request) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        // 1. If ISBN changed, verify uniqueness
        if (!existingBook.getIsbn().equalsIgnoreCase(request.getIsbn().trim())) {
            if (bookRepository.existsByIsbn(request.getIsbn().trim())) {
                throw new DuplicateResourceException("Another book already exists with ISBN: " + request.getIsbn());
            }
        }

        // 2. Validate copies against active borrowings
        int total = request.getTotalCopies();
        int available = request.getAvailableCopies() != null ? request.getAvailableCopies() : existingBook.getAvailableCopies();

        if (available > total) {
            throw new BadRequestException("Available copies (" + available + ") cannot exceed total copies (" + total + ")");
        }

        List<Borrowing> activeBorrowings = borrowingRepository.findByBookIdAndStatus(id, BorrowingStatus.BORROWED);
        int currentlyBorrowed = activeBorrowings.size();
        if (total < currentlyBorrowed) {
            throw new BadRequestException("Total copies (" + total + ") cannot be less than currently borrowed copies (" + currentlyBorrowed + ")");
        }
        if (available > total - currentlyBorrowed) {
            throw new BadRequestException("Available copies (" + available + ") cannot exceed " 
                    + (total - currentlyBorrowed) + " because " + currentlyBorrowed + " copies are currently borrowed");
        }

        existingBook.setTitle(request.getTitle().trim());
        existingBook.setAuthor(request.getAuthor().trim());
        existingBook.setIsbn(request.getIsbn().trim());
        existingBook.setCategory(request.getCategory().trim());
        existingBook.setTotalCopies(total);
        existingBook.setAvailableCopies(available);
        existingBook.setDescription(request.getDescription());

        Book updatedBook = bookRepository.save(existingBook);
        return BookResponseDTO.fromEntity(updatedBook);
    }

    /**
     * Delete a book if no copies are currently checked out.
     */
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        // Check if book has active borrowings
        List<Borrowing> activeBorrowings = borrowingRepository.findByBookIdAndStatus(id, BorrowingStatus.BORROWED);
        if (!activeBorrowings.isEmpty()) {
            throw new BadRequestException("Cannot delete book '" + book.getTitle() + "' because "
                    + activeBorrowings.size() + " copy/copies are currently borrowed");
        }

        bookRepository.delete(book);
    }

    private PagedResponse<BookResponseDTO> mapToPagedResponse(Page<Book> booksPage) {
        List<BookResponseDTO> content = booksPage.getContent().stream()
                .map(BookResponseDTO::fromEntity)
                .toList();

        return new PagedResponse<>(
                content,
                booksPage.getNumber(),
                booksPage.getSize(),
                booksPage.getTotalElements(),
                booksPage.getTotalPages(),
                booksPage.isLast()
        );
    }
}
