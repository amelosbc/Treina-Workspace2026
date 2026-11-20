package com.amelosbc.taskmanager.infrastructure;

/**
 * Custom exception thrown when a requested resource is not found.
 *
 * <p>This runtime exception is handled by the {@link GlobalExceptionHandler}
 * to return appropriate HTTP 404 responses.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
