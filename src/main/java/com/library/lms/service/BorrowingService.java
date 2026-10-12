package com.library.lms.service;

import com.library.lms.dto.response.BorrowingResponseDTO;
import com.library.lms.entity.Book;
import com.library.lms.entity.Borrowing;
import com.library.lms.entity.BorrowingStatus;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowingRepository;
import com.library.lms.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BorrowingService(BorrowingRepository borrowingRepository,
                            BookRepository bookRepository,
                            UserRepository userRepository) {
        this.borrowingRepository = borrowingRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    /**
     * Issue a book to the authenticated user.
     * Uses pessimistic locking on the Book entity to prevent race conditions and over-issuing.
     */
    @Transactional
    public BorrowingResponseDTO issueBook(Long bookId, String userEmail) {
        // 1. Resolve authenticated user identity
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // 2. Acquire database row-level lock on the book to prevent race conditions
        Book book = bookRepository.findByIdWithLock(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        // 3. Business Rule 1: A user cannot borrow an unavailable book
        if (book.getAvailableCopies() <= 0) {
            throw new BadRequestException("Book '" + book.getTitle() + "' is currently out of stock.");
        }

        // 4. Business Rule: Maximum 3 active loans limit per user
        long activeLoans = borrowingRepository.countByUserIdAndStatus(user.getId(), BorrowingStatus.BORROWED);
        if (activeLoans >= 3) {
            throw new BadRequestException("You have reached the maximum limit of 3 active borrowed books. Please return a book first.");
        }

        // 5. Business Rule: Prevent duplicate active borrowings of the same book
        if (borrowingRepository.existsByUserIdAndBookIdAndStatus(user.getId(), book.getId(), BorrowingStatus.BORROWED)) {
            throw new BadRequestException("You currently have an active borrowing for '" + book.getTitle() + "'. You cannot borrow multiple copies of the same title.");
        }

        // 6. Business Rule 2: Decrement available copies
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        // 7. Create borrowing record with 14-day due date
        LocalDateTime now = LocalDateTime.now();
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(user);
        borrowing.setBook(book);
        borrowing.setIssueDate(now);
        borrowing.setDueDate(now.plusDays(14));
        borrowing.setStatus(BorrowingStatus.BORROWED);

        Borrowing saved = borrowingRepository.save(borrowing);
        return BorrowingResponseDTO.fromEntity(saved);
    }

    /**
     * Return a borrowed book.
     * Verifies that the record is not already returned and validates user ownership or admin authority.
     */
    @Transactional
    public BorrowingResponseDTO returnBook(Long borrowingId, String currentUserEmail, boolean isAdmin) {
        // 1. Find borrowing record
        Borrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrowing transaction not found with ID: " + borrowingId));

        // 2. Business Rule 3: A borrowing record can be returned only once
        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new BadRequestException("This borrowing record has already been returned on " + borrowing.getReturnDate() + ".");
        }

        // 3. Business Rule 4: A user can return only their own borrowing unless administrator is authorized
        boolean isOwner = borrowing.getUser().getEmail().equalsIgnoreCase(currentUserEmail);
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Access Denied: You are not authorized to return another user's borrowed book.");
        }

        // 4. Acquire database row-level lock on the book
        Book book = bookRepository.findByIdWithLock(borrowing.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Book record not found for this transaction."));

        // 5. Business Rule 2: Increment available copies (up to totalCopies)
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepository.save(book);
        }

        // 6. Mark borrowing record as RETURNED
        borrowing.setStatus(BorrowingStatus.RETURNED);
        borrowing.setReturnDate(LocalDateTime.now());
        Borrowing updated = borrowingRepository.save(borrowing);

        return BorrowingResponseDTO.fromEntity(updated);
    }

    /**
     * List all borrowing history for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<BorrowingResponseDTO> getMyBorrowings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return borrowingRepository.findByUserIdOrderByIssueDateDesc(user.getId())
                .stream()
                .map(BorrowingResponseDTO::fromEntity)
                .toList();
    }

    /**
     * List only active borrowings (status = BORROWED) for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<BorrowingResponseDTO> getMyActiveBorrowings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return borrowingRepository.findByUserIdAndStatus(user.getId(), BorrowingStatus.BORROWED)
                .stream()
                .map(BorrowingResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Admin view: List all borrowings across the library.
     */
    @Transactional(readOnly = true)
    public List<BorrowingResponseDTO> getAllBorrowings() {
        return borrowingRepository.findAll(Sort.by(Sort.Direction.DESC, "issueDate"))
                .stream()
                .map(BorrowingResponseDTO::fromEntity)
                .toList();
    }
}
