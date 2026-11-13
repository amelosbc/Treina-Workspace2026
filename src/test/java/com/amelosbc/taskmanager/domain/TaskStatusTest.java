package com.amelosbc.taskmanager.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStatusTest {

    @Test
    @DisplayName("Should contain all expected Kanban statuses")
    void shouldContainAllExpectedStatuses() {
        assertThat(TaskStatus.values()).containsExactly(
                TaskStatus.TO_DO,
                TaskStatus.IN_PROGRESS,
                TaskStatus.IN_REVIEW,
                TaskStatus.DONE
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"TO_DO", "IN_PROGRESS", "IN_REVIEW", "DONE"})
    @DisplayName("Should correctly parse valid status names")
    void shouldParseValidStatusNames(String statusName) {
        TaskStatus status = TaskStatus.valueOf(statusName);
        assertThat(status.name()).isEqualTo(statusName);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid status name")
    void shouldThrowExceptionForInvalidStatus() {
        assertThatThrownBy(() -> TaskStatus.valueOf("INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
