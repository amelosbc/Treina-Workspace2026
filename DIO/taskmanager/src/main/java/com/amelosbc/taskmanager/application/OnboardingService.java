package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.domain.Board;
import com.amelosbc.taskmanager.domain.BoardRepository;
import com.amelosbc.taskmanager.domain.Task;
import com.amelosbc.taskmanager.domain.TaskPriority;
import com.amelosbc.taskmanager.domain.TaskRepository;
import com.amelosbc.taskmanager.domain.TaskStatus;
import com.amelosbc.taskmanager.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for onboarding new users by creating an initial Kanban board
 * populated with 3 introductory tasks.
 */
@Service
public class OnboardingService {

    public static final String INITIAL_BOARD_NAME = "Meu Primeiro Quadro";
    public static final String INITIAL_BOARD_DESC = "Quadro criado automaticamente para seus estudos e projetos";

    private final BoardRepository boardRepository;
    private final TaskRepository taskRepository;

    public OnboardingService(BoardRepository boardRepository, TaskRepository taskRepository) {
        this.boardRepository = boardRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Board createInitialBoardForUser(User user) {
        Board initialBoard = new Board(user, INITIAL_BOARD_NAME, INITIAL_BOARD_DESC);
        Board savedBoard = boardRepository.save(initialBoard);

        Task task1 = new Task(
            savedBoard,
            "Bem-vindo ao seu Kanban!",
            "Explore seus cartões",
            TaskStatus.TO_DO,
            TaskPriority.MEDIUM,
            0
        );

        Task task2 = new Task(
            savedBoard,
            "Arraste os cartões entre as colunas",
            "Mova cartões conforme seu fluxo",
            TaskStatus.IN_PROGRESS,
            TaskPriority.HIGH,
            0
        );

        Task task3 = new Task(
            savedBoard,
            "Conclua suas metas dos estudos DIO!",
            "Organize sua rotina diária",
            TaskStatus.DONE,
            TaskPriority.URGENT,
            0
        );

        taskRepository.saveAll(List.of(task1, task2, task3));
        return savedBoard;
    }
}
