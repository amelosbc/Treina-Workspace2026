package com.amelosbc.taskmanager.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskPriorityTest {

    @Test
    @DisplayName("Should contain all expected priorities")
    void shouldContainAllExpectedPriorities() {
        assertThat(TaskPriority.values()).containsExactly(
                TaskPriority.LOW,
                TaskPriority.MEDIUM,
                TaskPriority.HIGH,
                TaskPriority.URGENT
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    @DisplayName("Should correctly parse valid priority names")
    void shouldParseValidPriorityNames(String priorityName) {
        TaskPriority priority = TaskPriority.valueOf(priorityName);
        assertThat(priority.name()).isEqualTo(priorityName);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid priority name")
    void shouldThrowExceptionForInvalidPriority() {
        assertThatThrownBy(() -> TaskPriority.valueOf("INVALID_PRIORITY"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
