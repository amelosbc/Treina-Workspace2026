package com.amelosbc.taskmanager.application;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Task API input and output.
 *
 * <p>Uses a Java record for immutability and concise syntax.
 * Validation annotations are applied for input scenarios.</p>
 *
 * @param id          the task identifier (output only)
 * @param title       the task title (required)
 * @param description the task description (optional)
 * @param completed   whether the task is completed
 * @param createdAt   the creation timestamp (output only)
 * @param updatedAt   the last update timestamp (output only)
 */
public record TaskDTO(
        Long id,

        @NotBlank(message = "Title must not be blank")
        String title,

        String description,

        boolean completed,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}
