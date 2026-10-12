package com.library.lms.entity;

/**
 * Defines the user roles for authorization:
 * - USER: Normal library member (can search, borrow, return books)
 * - ADMIN: Librarian / Administrator (can manage the book catalog)
 */
public enum Role {
    USER,
    ADMIN
}
