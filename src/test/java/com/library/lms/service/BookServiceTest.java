package com.library.lms.service;

import com.library.lms.dto.request.BookRequestDTO;
import com.library.lms.dto.response.BookResponseDTO;
import com.library.lms.entity.Book;
import com.library.lms.entity.Borrowing;
import com.library.lms.entity.BorrowingStatus;
import com.library.lms.exception.BadRequestException;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook;
    private BookRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("Clean Code");
        sampleBook.setAuthor("Robert C. Martin");
        sampleBook.setIsbn("9780132350884");
        sampleBook.setCategory("Software Engineering");
        sampleBook.setTotalCopies(5);
        sampleBook.setAvailableCopies(5);
        sampleBook.setDescription("Agile craftsmanship handbook");

        validRequest = new BookRequestDTO(
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                "Software Engineering",
                5,
                5,
                "Agile craftsmanship handbook"
        );
    }

    @Test
    @DisplayName("Should successfully create a book when ISBN is unique and copies are valid")
    void testCreateBook_Success() {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        BookResponseDTO response = bookService.createBook(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getAvailableCopies()).isEqualTo(5);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when ISBN already exists")
    void testCreateBook_DuplicateIsbn_ThrowsConflict() {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when available copies exceed total copies")
    void testCreateBook_AvailableExceedsTotal_ThrowsBadRequest() {
        validRequest.setTotalCopies(3);
        validRequest.setAvailableCopies(5); // Invalid: 5 > 3

        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);

        assertThatThrownBy(() -> bookService.createBook(validRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed total copies");
    }

    @Test
    @DisplayName("Should throw BadRequestException when admin reduces total copies below active borrowings")
    void testUpdateBook_ReduceBelowCurrentlyBorrowed_ThrowsBadRequest() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        // Simulate 3 copies currently checked out by students
        Borrowing activeBorrowing = new Borrowing();
        activeBorrowing.setStatus(BorrowingStatus.BORROWED);
        when(borrowingRepository.findByBookIdAndStatus(1L, BorrowingStatus.BORROWED))
                .thenReturn(List.of(activeBorrowing, activeBorrowing, activeBorrowing));

        // Admin attempts to lower total copies to 2 while 3 are out
        validRequest.setTotalCopies(2);
        validRequest.setAvailableCopies(0);

        assertThatThrownBy(() -> bookService.updateBook(1L, validRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be less than currently borrowed copies (3)");
    }

    @Test
    @DisplayName("Should throw BadRequestException when attempting to delete a book with active borrowings")
    void testDeleteBook_ActiveBorrowingsExist_ThrowsBadRequest() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        Borrowing activeLoan = new Borrowing();
        activeLoan.setStatus(BorrowingStatus.BORROWED);
        when(borrowingRepository.findByBookIdAndStatus(1L, BorrowingStatus.BORROWED))
                .thenReturn(List.of(activeLoan));

        assertThatThrownBy(() -> bookService.deleteBook(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("currently borrowed");

        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("Should successfully delete a book when no active borrowings exist")
    void testDeleteBook_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(borrowingRepository.findByBookIdAndStatus(1L, BorrowingStatus.BORROWED))
                .thenReturn(Collections.emptyList());

        bookService.deleteBook(1L);

        verify(bookRepository, times(1)).delete(sampleBook);
    }
}
