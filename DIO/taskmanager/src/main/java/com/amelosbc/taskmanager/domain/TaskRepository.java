package com.amelosbc.taskmanager.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Task} entities.
 *
 * <p>Provides standard CRUD operations and query methods
 * for Task persistence.</p>
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}
