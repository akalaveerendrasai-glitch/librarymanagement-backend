package com.library.lms.controller;

import com.library.lms.dto.response.BorrowingResponseDTO;
import com.library.lms.service.BorrowingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {

    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    /**
     * Issue a book to the authenticated user.
     * Uses authentication.getName() (email) from the validated JWT token.
     */
    @PostMapping("/issue/{bookId}")
    public ResponseEntity<BorrowingResponseDTO> issueBook(@PathVariable Long bookId, Authentication authentication) {
        String userEmail = authentication.getName();
        BorrowingResponseDTO response = borrowingService.issueBook(bookId, userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Return a borrowed book.
     * Supports both /api/borrowings/{borrowingId}/return and /api/borrowings/return/{borrowingId}.
     */
    @PutMapping({"/{borrowingId}/return", "/return/{borrowingId}"})
    public ResponseEntity<BorrowingResponseDTO> returnBook(@PathVariable Long borrowingId, Authentication authentication) {
        String currentUserEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        BorrowingResponseDTO response = borrowingService.returnBook(borrowingId, currentUserEmail, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * List all borrowing records for the authenticated user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<BorrowingResponseDTO>> getMyBorrowings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<BorrowingResponseDTO> borrowings = borrowingService.getMyBorrowings(userEmail);
        return ResponseEntity.ok(borrowings);
    }

    /**
     * List currently active borrowings (status = BORROWED) for the authenticated user.
     */
    @GetMapping("/my/active")
    public ResponseEntity<List<BorrowingResponseDTO>> getMyActiveBorrowings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<BorrowingResponseDTO> activeBorrowings = borrowingService.getMyActiveBorrowings(userEmail);
        return ResponseEntity.ok(activeBorrowings);
    }

    /**
     * Admin view: List all borrowings across the entire library.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BorrowingResponseDTO>> getAllBorrowings() {
        List<BorrowingResponseDTO> allBorrowings = borrowingService.getAllBorrowings();
        return ResponseEntity.ok(allBorrowings);
    }
}
