package com.library.lms.dto.response;

import com.library.lms.entity.Borrowing;

import java.time.LocalDateTime;

public class BorrowingResponseDTO {

    private Long id;
    private BookSummaryDTO book;
    private UserSummaryDTO user;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;

    public BorrowingResponseDTO() {
    }

    public BorrowingResponseDTO(Long id, BookSummaryDTO book, UserSummaryDTO user,
                                LocalDateTime issueDate, LocalDateTime dueDate,
                                LocalDateTime returnDate, String status) {
        this.id = id;
        this.book = book;
        this.user = user;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public static BorrowingResponseDTO fromEntity(Borrowing borrowing) {
        if (borrowing == null) return null;

        BookSummaryDTO bookSummary = null;
        if (borrowing.getBook() != null) {
            bookSummary = new BookSummaryDTO(
                    borrowing.getBook().getId(),
                    borrowing.getBook().getTitle(),
                    borrowing.getBook().getAuthor(),
                    borrowing.getBook().getIsbn(),
                    borrowing.getBook().getCategory()
            );
        }

        UserSummaryDTO userSummary = null;
        if (borrowing.getUser() != null) {
            userSummary = new UserSummaryDTO(
                    borrowing.getUser().getId(),
                    borrowing.getUser().getName(),
                    borrowing.getUser().getEmail()
            );
        }

        return new BorrowingResponseDTO(
                borrowing.getId(),
                bookSummary,
                userSummary,
                borrowing.getIssueDate(),
                borrowing.getDueDate(),
                borrowing.getReturnDate(),
                borrowing.getStatus() != null ? borrowing.getStatus().name() : null
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BookSummaryDTO getBook() { return book; }
    public void setBook(BookSummaryDTO book) { this.book = book; }

    public UserSummaryDTO getUser() { return user; }
    public void setUser(UserSummaryDTO user) { this.user = user; }

    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Nested DTOs for safe serialization
    public static class BookSummaryDTO {
        private Long id;
        private String title;
        private String author;
        private String isbn;
        private String category;

        public BookSummaryDTO() {}

        public BookSummaryDTO(Long id, String title, String author, String isbn, String category) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.isbn = isbn;
            this.category = category;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getIsbn() { return isbn; }
        public String getCategory() { return category; }
    }

    public static class UserSummaryDTO {
        private Long id;
        private String name;
        private String email;

        public UserSummaryDTO() {}

        public UserSummaryDTO(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
}
