package com.library.lms.service;

import com.library.lms.dto.response.BorrowingResponseDTO;
import com.library.lms.entity.Book;
import com.library.lms.entity.Borrowing;
import com.library.lms.entity.BorrowingStatus;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowingRepository;
import com.library.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BorrowingService borrowingService;

    private User sampleUser;
    private User anotherUser;
    private Book sampleBook;
    private Borrowing sampleBorrowing;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Jane Doe");
        sampleUser.setEmail("jane@example.com");
        sampleUser.setRole(Role.USER);

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setName("John Smith");
        anotherUser.setEmail("john@example.com");
        anotherUser.setRole(Role.USER);

        sampleBook = new Book();
        sampleBook.setId(10L);
        sampleBook.setTitle("Head First Java");
        sampleBook.setAuthor("Kathy Sierra");
        sampleBook.setIsbn("9780596009205");
        sampleBook.setCategory("Programming");
        sampleBook.setTotalCopies(5);
        sampleBook.setAvailableCopies(5);

        sampleBorrowing = new Borrowing();
        sampleBorrowing.setId(100L);
        sampleBorrowing.setUser(sampleUser);
        sampleBorrowing.setBook(sampleBook);
        sampleBorrowing.setIssueDate(LocalDateTime.now().minusDays(5));
        sampleBorrowing.setDueDate(LocalDateTime.now().plusDays(9));
        sampleBorrowing.setStatus(BorrowingStatus.BORROWED);
    }

    @Test
    @DisplayName("Should successfully issue a book when copies are available and loan limits are respected")
    void testIssueBook_Success() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBook));
        when(borrowingRepository.countByUserIdAndStatus(1L, BorrowingStatus.BORROWED)).thenReturn(0L);
        when(borrowingRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, BorrowingStatus.BORROWED)).thenReturn(false);
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(sampleBorrowing);

        BorrowingResponseDTO response = borrowingService.issueBook(10L, "jane@example.com");

        assertThat(response).isNotNull();
        // Verifies available copies decremented from 5 to 4
        assertThat(sampleBook.getAvailableCopies()).isEqualTo(4);
        verify(bookRepository, times(1)).save(sampleBook);
        verify(borrowingRepository, times(1)).save(any(Borrowing.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when book has 0 available copies")
    void testIssueBook_OutOfStock_ThrowsBadRequest() {
        sampleBook.setAvailableCopies(0); // Out of stock

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBook));

        assertThatThrownBy(() -> borrowingService.issueBook(10L, "jane@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("currently out of stock");

        verify(bookRepository, never()).save(any(Book.class));
        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when student reaches maximum limit of 3 active loans")
    void testIssueBook_MaxThreeLoansLimit_ThrowsBadRequest() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBook));
        when(borrowingRepository.countByUserIdAndStatus(1L, BorrowingStatus.BORROWED)).thenReturn(3L);

        assertThatThrownBy(() -> borrowingService.issueBook(10L, "jane@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maximum limit of 3 active borrowed books");

        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when student tries to borrow a duplicate active copy of the same book")
    void testIssueBook_DuplicateActiveLoan_ThrowsBadRequest() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBook));
        when(borrowingRepository.countByUserIdAndStatus(1L, BorrowingStatus.BORROWED)).thenReturn(1L);
        when(borrowingRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, BorrowingStatus.BORROWED)).thenReturn(true);

        assertThatThrownBy(() -> borrowingService.issueBook(10L, "jane@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You currently have an active borrowing for 'Head First Java'");
    }

    @Test
    @DisplayName("Should successfully return a borrowed book and increment available copies")
    void testReturnBook_Success() {
        sampleBook.setAvailableCopies(4);

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(sampleBorrowing));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBook));
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(sampleBorrowing);

        BorrowingResponseDTO response = borrowingService.returnBook(100L, "jane@example.com", false);

        assertThat(response).isNotNull();
        // Verifies available copies incremented from 4 to 5
        assertThat(sampleBook.getAvailableCopies()).isEqualTo(5);
        assertThat(sampleBorrowing.getStatus()).isEqualTo(BorrowingStatus.RETURNED);
        assertThat(sampleBorrowing.getReturnDate()).isNotNull();

        verify(bookRepository, times(1)).save(sampleBook);
        verify(borrowingRepository, times(1)).save(sampleBorrowing);
    }

    @Test
    @DisplayName("Should throw BadRequestException when trying to return an already returned book")
    void testReturnBook_AlreadyReturned_ThrowsBadRequest() {
        sampleBorrowing.setStatus(BorrowingStatus.RETURNED);
        sampleBorrowing.setReturnDate(LocalDateTime.now().minusDays(1));

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(sampleBorrowing));

        assertThatThrownBy(() -> borrowingService.returnBook(100L, "jane@example.com", false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been returned");

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-admin attempts to return another user's loan")
    void testReturnBook_NonOwnerNonAdmin_ThrowsAccessDenied() {
        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(sampleBorrowing));

        // Another student attempts to return Jane's book
        assertThatThrownBy(() -> borrowingService.returnBook(100L, "john@example.com", false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to return another user's borrowed book");

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should allow administrator to return a book on behalf of any user")
    void testReturnBook_AdminCanReturnAnyUserBook_Success() {
        sampleBook.setAvailableCopies(4);

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(sampleBorrowing));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBook));
        when(borrowingRepository.save(any(Borrowing.class))).thenReturn(sampleBorrowing);

        // Admin returns the book (isAdmin = true)
        BorrowingResponseDTO response = borrowingService.returnBook(100L, "admin@library.com", true);

        assertThat(response).isNotNull();
        assertThat(sampleBook.getAvailableCopies()).isEqualTo(5);
        assertThat(sampleBorrowing.getStatus()).isEqualTo(BorrowingStatus.RETURNED);
    }
}
