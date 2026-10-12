package com.library.lms.repository;

import com.library.lms.entity.Borrowing;
import com.library.lms.entity.BorrowingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Borrowing transaction records.
 */
@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    /**
     * Get all borrowing records for a specific user, sorted from newest to oldest.
     */
    List<Borrowing> findByUserIdOrderByIssueDateDesc(Long userId);

    /**
     * Find borrowings for a specific user filtered by status (e.g., all active BORROWED books).
     */
    List<Borrowing> findByUserIdAndStatus(Long userId, BorrowingStatus status);

    /**
     * Find borrowings for a specific book filtered by status (useful to check active issues before deletion).
     */
    List<Borrowing> findByBookIdAndStatus(Long bookId, BorrowingStatus status);

    /**
     * Check if a user currently has an active borrowing for a specific book.
     */
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowingStatus status);

    /**
     * Count active borrowings for a user (useful to enforce a max-books limit if desired).
     */
    long countByUserIdAndStatus(Long userId, BorrowingStatus status);
}
