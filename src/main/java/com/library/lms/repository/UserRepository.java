package com.library.lms.repository;

import com.library.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique email address (used for login and duplicate check).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an account already exists with the given email address.
     */
    boolean existsByEmail(String email);
}
