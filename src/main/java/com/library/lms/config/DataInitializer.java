package com.library.lms.config;

import com.library.lms.entity.Book;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes default accounts and sample catalog books on first startup.
 * Ensures the system is immediately usable out-of-the-box for academic viva
 * and development without requiring manual SQL setup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BookRepository bookRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        String adminEmail = "admin@library.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User(
                    "Library Administrator",
                    adminEmail,
                    passwordEncoder.encode("Admin@123"),
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("==================================================================");
            System.out.println(">>> SEED: Initialized default ADMIN account: " + adminEmail);
            System.out.println(">>> Password: Admin@123");
            System.out.println("==================================================================");
        }

        String memberEmail = "jane@example.com";
        if (!userRepository.existsByEmail(memberEmail)) {
            User member = new User(
                    "Jane Doe",
                    memberEmail,
                    passwordEncoder.encode("Password@123"),
                    Role.USER
            );
            userRepository.save(member);
            System.out.println(">>> SEED: Initialized default MEMBER account: " + memberEmail);
            System.out.println(">>> Password: Password@123");
        }
    }

    private void seedBooks() {
        if (bookRepository.count() == 0) {
            List<Book> initialBooks = List.of(
                    new Book(
                            "Clean Code",
                            "Robert C. Martin",
                            "9780132350884",
                            "Software Engineering",
                            5,
                            5,
                            "A handbook of agile software craftsmanship explaining best practices for writing readable, maintainable code."
                    ),
                    new Book(
                            "The Pragmatic Programmer",
                            "Andrew Hunt, David Thomas",
                            "9780201616224",
                            "Software Engineering",
                            3,
                            3,
                            "Your journey to mastery: tips and practices for modern software development."
                    ),
                    new Book(
                            "Introduction to Algorithms",
                            "Thomas H. Cormen",
                            "9780262033848",
                            "Computer Science",
                            4,
                            4,
                            "Comprehensive textbook covering a broad range of algorithms in depth."
                    ),
                    new Book(
                            "Head First Java",
                            "Kathy Sierra, Bert Bates",
                            "9780596009205",
                            "Programming",
                            6,
                            6,
                            "A visual and engaging guide to learning object-oriented programming in Java."
                    ),
                    new Book(
                            "Database System Concepts",
                            "Abraham Silberschatz",
                            "9780073523323",
                            "Databases",
                            4,
                            4,
                            "Foundational principles of relational databases, SQL, and query processing."
                    )
            );

            bookRepository.saveAll(initialBooks);
            System.out.println(">>> SEED: Populated 5 initial academic books into library catalog.");
            System.out.println("==================================================================");
        }
    }
}
