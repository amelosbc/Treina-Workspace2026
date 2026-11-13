package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.domain.Task;
import com.amelosbc.taskmanager.domain.TaskRepository;
import com.amelosbc.taskmanager.infrastructure.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer responsible for Task business logic and CRUD operations.
 *
 * <p>Acts as an intermediary between the infrastructure (controller) layer
 * and the domain layer, handling data transformation between DTOs and entities.</p>
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * Constructs the TaskService with the required repository dependency.
     *
     * @param taskRepository the Task JPA repository
     */
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Retrieves all tasks from the database.
     *
     * @return a list of all tasks as DTOs
     */
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Retrieves a single task by its identifier.
     *
     * @param id the task ID
     * @return the task as a DTO
     * @throws ResourceNotFoundException if no task is found with the given ID
     */
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return toDTO(task);
    }

    /**
     * Creates a new task in the database.
     *
     * @param taskDTO the task data to create
     * @return the created task as a DTO (with generated ID and timestamps)
     */
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = new Task();
        task.setTitle(taskDTO.title());
        task.setDescription(taskDTO.description());
        task.setCompleted(taskDTO.completed());

        Task savedTask = taskRepository.save(task);
        return toDTO(savedTask);
    }

    /**
     * Updates an existing task with new data.
     *
     * @param id      the ID of the task to update
     * @param taskDTO the new task data
     * @return the updated task as a DTO
     * @throws ResourceNotFoundException if no task is found with the given ID
     */
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(taskDTO.title());
        task.setDescription(taskDTO.description());
        task.setCompleted(taskDTO.completed());

        Task updatedTask = taskRepository.save(task);
        return toDTO(updatedTask);
    }

    /**
     * Deletes a task by its identifier.
     *
     * @param id the ID of the task to delete
     * @throws ResourceNotFoundException if no task is found with the given ID
     */
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    /**
     * Toggles the completion status of a task.
     *
     * @param id the ID of the task to toggle
     * @return the updated task as a DTO
     * @throws ResourceNotFoundException if no task is found with the given ID
     */
    public TaskDTO toggleTaskCompletion(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setCompleted(!task.isCompleted());

        Task updatedTask = taskRepository.save(task);
        return toDTO(updatedTask);
    }

    /**
     * Converts a Task entity to a TaskDTO.
     *
     * @param task the Task entity
     * @return the corresponding TaskDTO
     */
    private TaskDTO toDTO(Task task) {
        return new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
