package com.library.lms.entity;

/**
 * Tracks the lifecycle status of a borrowed book:
 * - BORROWED: The book is currently issued to the member.
 * - RETURNED: The book has been checked back into the library.
 */
public enum BorrowingStatus {
    BORROWED,
    RETURNED
}
