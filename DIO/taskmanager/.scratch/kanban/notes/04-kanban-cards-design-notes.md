# Notas de Design e Especificação Técnica - Ticket 04
## Cartões do Kanban — Criação e Visualização Agrupada

**Referência do Ticket:** `.scratch/kanban/issues/04-kanban-cards-creation-and-view.md`  
**Documentos Base:** `SPEC.md` (Seções 3 e 4.2), `DESIGN.md` (Seções 1, 4.2 e 5), `GLOSSARY.md` (Task, TaskStatus, TaskPriority, Column Position, Soft Delete)  
**Dependência:** Ticket 03 (`.scratch/kanban/issues/03-boards-crud-and-onboarding.md`)

---

## 1. Visão Geral e Responsabilidades do Domínio

O Ticket 04 estabelece o núcleo operacional do TaskManager: o ciclo de vida dos **Cartões de Tarefa (Tasks)** dentro do fluxo visual do **Kanban**.

Enquanto o Ticket 03 introduziu o contêiner de alto nível (`Board`), o Ticket 04 evolui a entidade `Task` para se integrar completamente à metodologia Kanban, introduzindo:
1. **Pertença Estrita a um Quadro:** Cada tarefa está associada obrigatoriamente a um `Board` (`board_id`).
2. **Ciclo de Vida por Status:** Transição do modelo booleano legado (`completed: boolean`) para as 4 colunas formais de fluxo contínuo: `TO_DO`, `IN_PROGRESS`, `IN_REVIEW` e `DONE` (`TaskStatus`).
3. **Priorização e Prazos:** Introdução de níveis de urgência (`TaskPriority`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`) e data limite de entrega opcional (`dueDate: LocalDate`).
4. **Posicionamento Sequencial Contíguo (Dense Position):** Cada coluna do quadro mantém um índice sequencial (`0, 1, 2...`) que governa a ordem visual dos cartões. Novas tarefas recebem automaticamente a próxima posição disponível na coluna (`max(position) + 1`).
5. **Visão Agrupada por Colunas (Kanban Board View):** Retorno estruturado do quadro com um mapa contendo todas as 4 colunas garantidas, cada qual com sua lista de cartões ordenada crescentemente por `position`.
6. **Consultas Flexíveis e Filtros:** Capacidade de listar tarefas de um quadro filtrando dinamicamente por status e prioridade.
7. **Isolamento Multitenant Rigoroso:** Todas as operações verificam se o `Board` pertence ao usuário autenticado antes de qualquer consulta ou inserção, retornando `HTTP 404 Not Found` em acessos não autorizados para prevenir enumeração de recursos.

---

## 2. Evolução da Entidade de Domínio `Task`

### 2.1. Estrutura do Modelo de Dados

A tabela relacional `tasks` é expandida para suportar as regras de negócio do SaaS Kanban:

| Coluna | Tipo SQL | Restrições | Descrição |
|---|---|---|---|
| `id` | `BIGINT` | `PK, AUTO_INCREMENT` | Identificador único da tarefa |
| `board_id` | `BIGINT` | `FK, NOT NULL` | Chave estrangeira para o quadro dono (`boards.id`) |
| `title` | `VARCHAR(200)` | `NOT NULL` | Título sucinto da tarefa (obrigatório) |
| `description` | `VARCHAR(4000)` | `NULLABLE` | Conteúdo detalhado da tarefa formatado em Markdown |
| `status` | `VARCHAR(30)` | `NOT NULL, DEFAULT 'TO_DO'` | Coluna atual do fluxo Kanban (`TaskStatus`) |
| `priority` | `VARCHAR(30)` | `NOT NULL, DEFAULT 'MEDIUM'` | Grau de urgência da tarefa (`TaskPriority`) |
| `position` | `INTEGER` | `NOT NULL, DEFAULT 0` | Índice sequencial da tarefa dentro de sua coluna |
| `due_date` | `DATE` | `NULLABLE` | Prazo limite de conclusão (opcional) |
| `created_at` | `TIMESTAMP` | `NOT NULL, UPDATABLE=FALSE` | Data/hora de criação gerenciada via `@PrePersist` |
| `updated_at` | `TIMESTAMP` | `NULLABLE` | Data/hora da última alteração via `@PreUpdate` |
| `deleted_at` | `TIMESTAMP` | `NULLABLE` | Timestamp para Soft Delete (`NULL` para tarefas ativas) |

> **Nota sobre o campo legado `completed`:** O modelo simplificado inicial utilizava `completed: boolean`. Com a introdução do Kanban, o estado de conclusão é unificado e governado por `status == TaskStatus.DONE`. Para manter compatibilidade com eventuais chamadas utilitárias do domínio, mantém-se um método helper `public boolean isCompleted() { return this.status == TaskStatus.DONE; }`, mas a persistência no banco passa a ser exclusivamente via coluna `status`.

### 2.2. Código de Referência da Entidade JPA (`com.amelosbc.taskmanager.domain.Task`)

```java
package com.amelosbc.taskmanager.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade de domínio que representa um Cartão de Tarefa no fluxo Kanban.
 */
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_board_status", columnList = "board_id, status, deleted_at"),
    @Index(name = "idx_task_board_position", columnList = "board_id, status, position")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @NotBlank(message = "O título da tarefa é obrigatório")
    @Size(max = 200, message = "O título da tarefa deve ter no máximo 200 caracteres")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
    @Column(name = "description", length = 4000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status = TaskStatus.TO_DO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @NotNull
    @Column(name = "position", nullable = false)
    private Integer position = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Construtor padrão exigido pela JPA.
     */
    public Task() {
    }

    /**
     * Construtor parametrizado completo para novas tarefas.
     */
    public Task(Board board, String title, String description, TaskStatus status,
                TaskPriority priority, Integer position, LocalDate dueDate) {
        this.board = board;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : TaskStatus.TO_DO;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.position = position != null ? position : 0;
        this.dueDate = dueDate;
    }

    /**
     * Construtor simplificado (usado por exemplo no Onboarding).
     */
    public Task(Board board, String title, String description, TaskStatus status,
                TaskPriority priority, Integer position) {
        this(board, title, description, status, priority, position, null);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TaskStatus.TO_DO;
        }
        if (this.priority == null) {
            this.priority = TaskPriority.MEDIUM;
        }
        if (this.position == null) {
            this.position = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Helpers de Domínio e Soft Delete ---

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == TaskStatus.DONE;
    }

    // --- Getters e Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Board getBoard() { return board; }
    public void setBoard(Board board) { this.board = board; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
```

---

## 3. Repositório `TaskRepository` e Consultas Especializadas

### 3.1. Métodos Necessários no `TaskRepository`

O repositório combina convenções do Spring Data JPA com `@Query` otimizada para o cálculo de posição:

```java
package com.amelosbc.taskmanager.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Retorna todas as tarefas ativas de um quadro específico, ordenadas por posição.
     */
    List<Task> findAllByBoardIdAndDeletedAtIsNullOrderByPositionAsc(Long boardId);

    /**
     * Retorna todas as tarefas ativas de uma coluna de status específica em um quadro.
     */
    List<Task> findAllByBoardIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(Long boardId, TaskStatus status);

    /**
     * Retorna uma tarefa ativa específica de um quadro por seus IDs.
     */
    Optional<Task> findByIdAndBoardIdAndDeletedAtIsNull(Long id, Long boardId);

    /**
     * Retorna a maior posição atual entre as tarefas ativas de uma coluna.
     * Retorna null caso a coluna não contenha nenhuma tarefa ativa.
     */
    @Query("SELECT MAX(t.position) FROM Task t WHERE t.board.id = :boardId AND t.status = :status AND t.deletedAt IS NULL")
    Integer findMaxPositionByBoardIdAndStatus(@Param("boardId") Long boardId, @Param("status") TaskStatus status);

    /**
     * Busca tarefas ativas com filtros opcionais combinados (status e prioridade).
     */
    @Query("SELECT t FROM Task t WHERE t.board.id = :boardId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND t.deletedAt IS NULL " +
           "ORDER BY t.status ASC, t.position ASC")
    List<Task> findTasksWithFilters(@Param("boardId") Long boardId,
                                    @Param("status") TaskStatus status,
                                    @Param("priority") TaskPriority priority);
}
```

---

## 4. Contratos de DTO (Data Transfer Objects)

Os DTOs são modelados como Java Records imutáveis, pertencentes ao pacote `com.amelosbc.taskmanager.application.dto`.

### 4.1. `TaskCreateRequestDTO` (Payload de Criação)

```java
package com.amelosbc.taskmanager.application.dto;

import com.amelosbc.taskmanager.domain.TaskPriority;
import com.amelosbc.taskmanager.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Payload para criação de uma nova tarefa em um quadro Kanban")
public record TaskCreateRequestDTO(

    @Schema(description = "Título da tarefa", example = "Configurar autenticação Clerk", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O título da tarefa é obrigatório")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    String title,

    @Schema(description = "Descrição detalhada da tarefa em formato Markdown", example = "Integrar o SDK do Clerk e configurar os filtros JWT.")
    @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
    String description,

    @Schema(description = "Status inicial da tarefa (default: TO_DO)", example = "TO_DO", defaultValue = "TO_DO")
    TaskStatus status,

    @Schema(description = "Nível de prioridade da tarefa (default: MEDIUM)", example = "HIGH", defaultValue = "MEDIUM")
    TaskPriority priority,

    @Schema(description = "Data limite de conclusão (opcional)", example = "2026-12-31")
    LocalDate dueDate
) {
    /**
     * Retorna o status informado ou o default TO_DO se for nulo.
     */
    public TaskStatus statusOrDefault() {
        return status != null ? status : TaskStatus.TO_DO;
    }

    /**
     * Retorna a prioridade informada ou o default MEDIUM se for nulo.
     */
    public TaskPriority priorityOrDefault() {
        return priority != null ? priority : TaskPriority.MEDIUM;
    }
}
```

### 4.2. `TaskDetailResponseDTO` (Retorno Detalhado da Tarefa)

```java
package com.amelosbc.taskmanager.application.dto;

import com.amelosbc.taskmanager.domain.Task;
import com.amelosbc.taskmanager.domain.TaskPriority;
import com.amelosbc.taskmanager.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Representação detalhada de um cartão de tarefa")
public record TaskDetailResponseDTO(
    @Schema(description = "ID único da tarefa", example = "101")
    Long id,

    @Schema(description = "ID do quadro a que a tarefa pertence", example = "1")
    Long boardId,

    @Schema(description = "Título da tarefa", example = "Implementar JWT Filter")
    String title,

    @Schema(description = "Descrição detalhada em Markdown", example = "Detalhes da implementação")
    String description,

    @Schema(description = "Coluna atual de status no Kanban", example = "TO_DO")
    TaskStatus status,

    @Schema(description = "Nível de prioridade", example = "HIGH")
    TaskPriority priority,

    @Schema(description = "Posição relativa dentro da coluna", example = "0")
    Integer position,

    @Schema(description = "Data limite de conclusão", example = "2026-12-31")
    LocalDate dueDate,

    @Schema(description = "Data e hora de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data e hora da última modificação")
    LocalDateTime updatedAt
) {
    public static TaskDetailResponseDTO fromDomain(Task task) {
        return new TaskDetailResponseDTO(
            task.getId(),
            task.getBoard() != null ? task.getBoard().getId() : null,
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getPosition(),
            task.getDueDate(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
```

### 4.3. `BoardKanbanViewDTO` (Visão Estruturada do Quadro)

A visão do quadro agrupa as tarefas por colunas de status. É um requisito fundamental do domínio que **todas as 4 colunas de status existam no mapa**, mesmo quando estiverem vazias (`[]`):

```java
package com.amelosbc.taskmanager.application.dto;

import com.amelosbc.taskmanager.domain.Board;
import com.amelosbc.taskmanager.domain.Task;
import com.amelosbc.taskmanager.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;

@Schema(description = "Visualização do Quadro Kanban com tarefas agrupadas por colunas de status")
public record BoardKanbanViewDTO(
    @Schema(description = "ID do quadro", example = "1")
    Long boardId,

    @Schema(description = "Nome do quadro", example = "Estudos DIO - Backend")
    String boardName,

    @Schema(description = "Descrição do quadro", example = "Quadro de estudos de arquitetura")
    String boardDescription,

    @Schema(description = "Colunas do Kanban contendo as tarefas ordenadas por posição")
    Map<TaskStatus, List<TaskDetailResponseDTO>> columns
) {
    /**
     * Monta a visão Kanban agrupando as tarefas e garantindo a presença
     * de todas as 4 colunas (TO_DO, IN_PROGRESS, IN_REVIEW, DONE), ordenadas por posição.
     */
    public static BoardKanbanViewDTO fromDomain(Board board, List<Task> tasks) {
        // Inicializa mapa preservando a ordem canônica dos enums
        Map<TaskStatus, List<TaskDetailResponseDTO>> columns = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            columns.put(status, new ArrayList<>());
        }

        // Agrupa as tarefas já ordenadas por posição
        if (tasks != null) {
            for (Task task : tasks) {
                if (!task.isDeleted()) {
                    TaskStatus status = task.getStatus();
                    columns.computeIfAbsent(status, k -> new ArrayList<>())
                           .add(TaskDetailResponseDTO.fromDomain(task));
                }
            }
        }

        return new BoardKanbanViewDTO(
            board.getId(),
            board.getName(),
            board.getDescription(),
            columns
        );
    }
}
```

---

## 5. Lógica de Negócio e Casos de Uso no `TaskService`

### 5.1. Algoritmo de Cálculo de Posição Automática

Conforme a especificação de *Dense Reordering* e *Column Position* (`GLOSSARY.md` e Ticket 04):
- Ao adicionar um novo cartão na coluna `status`, o sistema consulta a maior posição existente entre as tarefas ativas daquela coluna.
- Se a coluna não contiver nenhuma tarefa ativa (`maxPosition == null`), a nova tarefa recebe `position = 0`.
- Se a coluna já contiver tarefas, a nova tarefa recebe `position = maxPosition + 1`.

```mermaid
flowchart TD
    Start["Nova Tarefa (status: S)"] --> CheckBoard["Validar se Board existe e pertence ao userId"]
    CheckBoard -->|Não| Throw404["Lança ResourceNotFoundException (404)"]
    CheckBoard -->|Sim| QueryMax["findMaxPositionByBoardIdAndStatus(boardId, S)"]
    QueryMax --> NullCheck{"maxPosition é null?"}
    NullCheck -->|Sim (coluna vazia)| PosZero["position = 0"]
    NullCheck -->|Não| PosNext["position = maxPosition + 1"]
    PosZero --> SaveTask["taskRepository.save(task)"]
    PosNext --> SaveTask
    SaveTask --> ReturnDTO["Retornar TaskDetailResponseDTO"]
```

### 5.2. Métodos de Aplicação do `TaskService`

```java
package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.application.dto.BoardKanbanViewDTO;
import com.amelosbc.taskmanager.application.dto.TaskCreateRequestDTO;
import com.amelosbc.taskmanager.application.dto.TaskDetailResponseDTO;
import com.amelosbc.taskmanager.domain.*;
import com.amelosbc.taskmanager.infrastructure.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;

    public TaskService(TaskRepository taskRepository, BoardRepository boardRepository) {
        this.taskRepository = taskRepository;
        this.boardRepository = boardRepository;
    }

    /**
     * Cria uma nova tarefa associada a um quadro específico.
     * Atribui automaticamente a próxima posição disponível na coluna de destino.
     */
    @Transactional
    public TaskDetailResponseDTO createTaskInBoard(Long boardId, TaskCreateRequestDTO dto, Long userId) {
        // Valida posse do quadro (Tenant Isolation)
        Board board = boardRepository.findByIdAndUserIdAndDeletedAtIsNull(boardId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Quadro não encontrado com o id: " + boardId));

        TaskStatus status = dto.statusOrDefault();
        TaskPriority priority = dto.priorityOrDefault();

        // Cálculo de posição sequencial no final da coluna
        Integer maxPosition = taskRepository.findMaxPositionByBoardIdAndStatus(boardId, status);
        int nextPosition = (maxPosition != null) ? maxPosition + 1 : 0;

        Task task = new Task(
            board,
            dto.title(),
            dto.description(),
            status,
            priority,
            nextPosition,
            dto.dueDate()
        );

        Task savedTask = taskRepository.save(task);
        return TaskDetailResponseDTO.fromDomain(savedTask);
    }

    /**
     * Retorna a visão completa do quadro Kanban com as colunas agrupadas e ordenadas por posição.
     */
    @Transactional(readOnly = true)
    public BoardKanbanViewDTO getBoardKanbanView(Long boardId, Long userId) {
        Board board = boardRepository.findByIdAndUserIdAndDeletedAtIsNull(boardId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Quadro não encontrado com o id: " + boardId));

        List<Task> activeTasks = taskRepository.findAllByBoardIdAndDeletedAtIsNullOrderByPositionAsc(boardId);
        return BoardKanbanViewDTO.fromDomain(board, activeTasks);
    }

    /**
     * Lista tarefas de um quadro com filtros opcionais por status e prioridade.
     */
    @Transactional(readOnly = true)
    public List<TaskDetailResponseDTO> listBoardTasks(Long boardId, TaskStatus status, TaskPriority priority, Long userId) {
        // Valida posse do quadro
        if (!boardRepository.existsByIdAndUserIdAndDeletedAtIsNull(boardId, userId)) {
            throw new ResourceNotFoundException("Quadro não encontrado com o id: " + boardId);
        }

        List<Task> tasks = taskRepository.findTasksWithFilters(boardId, status, priority);
        return tasks.stream()
            .map(TaskDetailResponseDTO::fromDomain)
            .toList();
    }

    /**
     * Obtém os detalhes de uma tarefa específica dentro de um quadro.
     */
    @Transactional(readOnly = true)
    public TaskDetailResponseDTO getTaskDetail(Long boardId, Long taskId, Long userId) {
        if (!boardRepository.existsByIdAndUserIdAndDeletedAtIsNull(boardId, userId)) {
            throw new ResourceNotFoundException("Quadro não encontrado com o id: " + boardId);
        }

        Task task = taskRepository.findByIdAndBoardIdAndDeletedAtIsNull(taskId, boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com o id: " + taskId));

        return TaskDetailResponseDTO.fromDomain(task);
    }
}
```

---

## 6. Endpoints REST e OpenAPI (`BoardTaskController`)

Conforme a especificação em `SPEC.md` (Seção 4.1 e 4.2), os endpoints REST do Kanban são organizados sob `/api/boards`:

```
POST /api/boards/{boardId}/tasks         -> Cria tarefa no quadro e aloca próxima posição
GET  /api/boards/{boardId}               -> Retorna o quadro com tarefas agrupadas por status (Kanban View)
GET  /api/boards/{boardId}/tasks         -> Lista tarefas do quadro com filtros opcionais (?status=&priority=)
GET  /api/boards/{boardId}/tasks/{taskId} -> Detalhes de uma tarefa específica
```

### 6.1. Código de Referência do `BoardTaskController`

```java
package com.amelosbc.taskmanager.infrastructure;

import com.amelosbc.taskmanager.application.TaskService;
import com.amelosbc.taskmanager.application.dto.BoardKanbanViewDTO;
import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import com.amelosbc.taskmanager.application.dto.TaskCreateRequestDTO;
import com.amelosbc.taskmanager.application.dto.TaskDetailResponseDTO;
import com.amelosbc.taskmanager.domain.TaskPriority;
import com.amelosbc.taskmanager.domain.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}")
@Tag(name = "Board Tasks", description = "Endpoints para gerenciamento e visualização de tarefas no quadro Kanban")
public class BoardTaskController {

    private final TaskService taskService;

    public BoardTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/tasks")
    @Operation(summary = "Criar nova tarefa no quadro",
               description = "Cria um novo cartão associado ao quadro e calcula automaticamente a próxima posição no final da coluna indicada.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Quadro não encontrado ou não pertence ao usuário")
    })
    public ResponseEntity<TaskDetailResponseDTO> createTask(
            @PathVariable Long boardId,
            @Valid @RequestBody TaskCreateRequestDTO requestDTO,
            @AuthenticationPrincipal CurrentUserContext currentUser) {

        TaskDetailResponseDTO created = taskService.createTaskInBoard(boardId, requestDTO, currentUser.userId());
        URI location = URI.create(String.format("/api/boards/%d/tasks/%d", boardId, created.id()));
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Visualizar Quadro Kanban agrupado",
               description = "Retorna os detalhes do quadro com todas as tarefas ativas agrupadas em suas respectivas colunas de status e ordenadas por posição.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Visão Kanban retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Quadro não encontrado ou não pertence ao usuário")
    })
    public ResponseEntity<BoardKanbanViewDTO> getBoardKanbanView(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CurrentUserContext currentUser) {

        BoardKanbanViewDTO view = taskService.getBoardKanbanView(boardId, currentUser.userId());
        return ResponseEntity.ok(view);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Listar tarefas do quadro com filtros opcionais",
               description = "Retorna uma lista plana das tarefas do quadro com opção de filtrar por status e/ou prioridade.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefas retornadas com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Quadro não encontrado ou não pertence ao usuário")
    })
    public ResponseEntity<List<TaskDetailResponseDTO>> listTasks(
            @PathVariable Long boardId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @AuthenticationPrincipal CurrentUserContext currentUser) {

        List<TaskDetailResponseDTO> tasks = taskService.listBoardTasks(boardId, status, priority, currentUser.userId());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Detalhes de uma tarefa do quadro",
               description = "Retorna informações completas de um cartão específico pertencente ao quadro informado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa encontrada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Quadro ou tarefa não encontrada")
    })
    public ResponseEntity<TaskDetailResponseDTO> getTaskDetail(
            @PathVariable Long boardId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal CurrentUserContext currentUser) {

        TaskDetailResponseDTO task = taskService.getTaskDetail(boardId, taskId, currentUser.userId());
        return ResponseEntity.ok(task);
    }
}
```

> **Nota de Roteamento:** `GET /api/boards/{id}` na Seção 4.1 do `SPEC.md` é especificado como: *"Obtém o quadro com suas tarefas ativas agrupadas pelas 4 colunas de status"*. Isso se alinha com `getBoardKanbanView`. Já os metadados brutos do quadro para edição continuam disponíveis em `BoardController` se necessário, ou `BoardKanbanViewDTO` pode ser a resposta canônica de `GET /api/boards/{id}`.

---

## 7. Matriz de Cenários de Teste MockMvc (`BoardTaskIntegrationTest`)

A suíte de testes de integração deve ser implementada no pacote `com.amelosbc.taskmanager.infrastructure.controllers` utilizando:
- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- Injeção de autenticação via header `X-Mock-User` para simular usuários distintos (`user_alice` e `user_bob`).

### 7.1. Matriz de Cenários e Cobertura

| ID | Cenário | Método / Endpoint | Entrada / Condição | Comportamento Esperado | Código HTTP |
|---|---|---|---|---|---|
| **TC-01** | Criação com payload completo | `POST /api/boards/{id}/tasks` | Título, descrição, status `IN_PROGRESS`, prioridade `HIGH`, dueDate | Tarefa criada com dados persistidos e location header correto | `201 CREATED` |
| **TC-02** | Criação com valores padrão (defaults) | `POST /api/boards/{id}/tasks` | Apenas título fornecido | `status=TO_DO`, `priority=MEDIUM`, `position=0` | `201 CREATED` |
| **TC-03** | Atribuição sequencial de posições na mesma coluna | `POST /api/boards/{id}/tasks` | Criação consecutiva de 3 tarefas com `status=TO_DO` | 1ª tarefa: `pos=0`<br/>2ª tarefa: `pos=1`<br/>3ª tarefa: `pos=2` | `201 CREATED` |
| **TC-04** | Posições independentes por coluna de status | `POST /api/boards/{id}/tasks` | Criar 2 tarefas em `TO_DO` e 1 tarefa em `DONE` | `TO_DO`: `pos=0` e `pos=1`<br/>`DONE`: `pos=0` | `201 CREATED` |
| **TC-05** | Validação: título em branco | `POST /api/boards/{id}/tasks` | Payload com `title: ""` ou em branco | Erro de validação de campo obrigatório | `400 BAD REQUEST` |
| **TC-06** | Visualização Kanban completa | `GET /api/boards/{id}` | Quadro com tarefas em colunas variadas | Mapa com as 4 colunas (`TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`) presentes | `200 OK` |
| **TC-07** | Ordenação interna por posição no Kanban | `GET /api/boards/{id}` | Várias tarefas na mesma coluna | Lista da coluna ordenada estritamente por `position ASC` | `200 OK` |
| **TC-08** | Colunas vazias no Kanban | `GET /api/boards/{id}` | Quadro recém-criado sem tarefas | As 4 colunas presentes no mapa com listas vazias `[]` | `200 OK` |
| **TC-09** | Exclusão lógica: tarefas deletadas não aparecem | `GET /api/boards/{id}` | Tarefa marcada com `deleted_at != null` | Tarefa ocultada da visualização do quadro | `200 OK` |
| **TC-10** | Listagem plana sem filtros | `GET /api/boards/{id}/tasks` | N/A | Retorna todas as tarefas ativas do quadro | `200 OK` |
| **TC-11** | Listagem filtrada por status | `GET /api/boards/{id}/tasks?status=IN_PROGRESS` | Query param `status=IN_PROGRESS` | Retorna somente tarefas com o status solicitado | `200 OK` |
| **TC-12** | Listagem filtrada por prioridade | `GET /api/boards/{id}/tasks?priority=HIGH` | Query param `priority=HIGH` | Retorna somente tarefas com a prioridade solicitada | `200 OK` |
| **TC-13** | Listagem com filtros combinados | `GET /api/boards/{id}/tasks?status=TO_DO&priority=URGENT` | Ambos os parâmetros | Retorna a interseção estrita dos filtros | `200 OK` |
| **TC-14** | Detalhes de tarefa existente | `GET /api/boards/{id}/tasks/{taskId}` | ID de tarefa válida no quadro | Retorna o `TaskDetailResponseDTO` completo | `200 OK` |
| **TC-15** | Tarefa de outro quadro (Cross-board Mismatch) | `GET /api/boards/{idA}/tasks/{taskIdB}` | Tarefa pertence ao quadro B, mas requisitada no quadro A | Não encontra a tarefa associada ao quadro A | `404 NOT FOUND` |
| **TC-16** | Isolamento Tenant: Usuário B criando tarefa no quadro do Usuário A | `POST /api/boards/{idA}/tasks` (Usuário B) | Header `X-Mock-User: user_bob` para quadro do `user_alice` | Proteção de dados sem vazar existência do quadro | `404 NOT FOUND` |
| **TC-17** | Isolamento Tenant: Usuário B visualizando quadro do Usuário A | `GET /api/boards/{idA}` (Usuário B) | Header `X-Mock-User: user_bob` para quadro do `user_alice` | Proteção de leitura não autorizada | `404 NOT FOUND` |
| **TC-18** | Isolamento Tenant: Usuário B listando tarefas do quadro do Usuário A | `GET /api/boards/{idA}/tasks` (Usuário B) | Header `X-Mock-User: user_bob` para quadro do `user_alice` | Bloqueio de acesso | `404 NOT FOUND` |
| **TC-19** | Não autenticado | Qualquer endpoint `/api/boards/**` | Requisição sem header de autenticação | Resposta de autorização necessária | `401 UNAUTHORIZED` |

### 7.2. Código do Teste de Integração de Referência (`BoardTaskIntegrationTest.java`)

```java
package com.amelosbc.taskmanager.infrastructure.controllers;

import com.amelosbc.taskmanager.application.dto.TaskCreateRequestDTO;
import com.amelosbc.taskmanager.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoardTaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User alice;
    private User bob;
    private Board aliceBoard;
    private Board bobBoard;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(new User("clerk_alice", "alice@example.com", "Alice"));
        bob = userRepository.save(new User("clerk_bob", "bob@example.com", "Bob"));

        aliceBoard = boardRepository.save(new Board(alice, "Quadro da Alice", "Quadro privado da Alice"));
        bobBoard = boardRepository.save(new Board(bob, "Quadro do Bob", "Quadro privado do Bob"));
    }

    @Test
    @DisplayName("Deve criar uma tarefa com sucesso atribuindo próxima posição e valores padrão")
    void shouldCreateTaskWithAutoIncrementedPositionAndDefaults() throws Exception {
        TaskCreateRequestDTO request = new TaskCreateRequestDTO(
            "Configurar Docker",
            "Criar Dockerfile otimizado",
            null, // default TO_DO
            null, // default MEDIUM
            LocalDate.of(2026, 12, 1)
        );

        mockMvc.perform(post("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.boardId").value(aliceBoard.getId()))
            .andExpect(jsonPath("$.title").value("Configurar Docker"))
            .andExpect(jsonPath("$.status").value("TO_DO"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"))
            .andExpect(jsonPath("$.position").value(0))
            .andExpect(jsonPath("$.dueDate").value("2026-12-01"));

        // Criar segunda tarefa na mesma coluna TO_DO
        TaskCreateRequestDTO secondRequest = new TaskCreateRequestDTO(
            "Criar docker-compose.yml",
            null,
            TaskStatus.TO_DO,
            TaskPriority.HIGH,
            null
        );

        mockMvc.perform(post("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.position").value(1))
            .andExpect(jsonPath("$.status").value("TO_DO"));
    }

    @Test
    @DisplayName("Deve manter posições independentes para diferentes colunas de status")
    void shouldMaintainIndependentPositionsPerStatusColumn() throws Exception {
        // Tarefa 1 em TO_DO -> pos 0
        taskRepository.save(new Task(aliceBoard, "T1", null, TaskStatus.TO_DO, TaskPriority.LOW, 0));
        // Tarefa 2 em TO_DO -> pos 1
        taskRepository.save(new Task(aliceBoard, "T2", null, TaskStatus.TO_DO, TaskPriority.LOW, 1));

        // Nova tarefa criada via API com status IN_PROGRESS deve receber pos 0
        TaskCreateRequestDTO request = new TaskCreateRequestDTO("T3 em progresso", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null);

        mockMvc.perform(post("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    @DisplayName("Deve retornar erro 400 Bad Request ao tentar criar tarefa com título em branco")
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        TaskCreateRequestDTO invalidRequest = new TaskCreateRequestDTO(
            "   ",
            "Sem título válido",
            TaskStatus.TO_DO,
            TaskPriority.LOW,
            null
        );

        mockMvc.perform(post("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar a visualização Kanban com todas as 4 colunas e tarefas ordenadas por posição")
    void shouldReturnKanbanViewWithAllColumnsOrderedByPosition() throws Exception {
        taskRepository.save(new Task(aliceBoard, "Task 1", null, TaskStatus.TO_DO, TaskPriority.LOW, 1));
        taskRepository.save(new Task(aliceBoard, "Task 0", null, TaskStatus.TO_DO, TaskPriority.MEDIUM, 0));
        taskRepository.save(new Task(aliceBoard, "Em andamento", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, 0));

        mockMvc.perform(get("/api/boards/{boardId}", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.boardId").value(aliceBoard.getId()))
            .andExpect(jsonPath("$.boardName").value("Quadro da Alice"))
            // Todas as 4 colunas devem existir
            .andExpect(jsonPath("$.columns.TO_DO").isArray())
            .andExpect(jsonPath("$.columns.IN_PROGRESS").isArray())
            .andExpect(jsonPath("$.columns.IN_REVIEW").isArray())
            .andExpect(jsonPath("$.columns.DONE").isArray())
            // Ordem na coluna TO_DO deve ser pos 0 antes de pos 1
            .andExpect(jsonPath("$.columns.TO_DO[0].title").value("Task 0"))
            .andExpect(jsonPath("$.columns.TO_DO[0].position").value(0))
            .andExpect(jsonPath("$.columns.TO_DO[1].title").value("Task 1"))
            .andExpect(jsonPath("$.columns.TO_DO[1].position").value(1))
            // Colunas vazias devem ser arrays vazios
            .andExpect(jsonPath("$.columns.IN_REVIEW", hasSize(0)))
            .andExpect(jsonPath("$.columns.DONE", hasSize(0)));
    }

    @Test
    @DisplayName("Não deve retornar tarefas com soft delete na visualização Kanban")
    void shouldNotIncludeSoftDeletedTasksInKanbanView() throws Exception {
        Task activeTask = new Task(aliceBoard, "Ativa", null, TaskStatus.TO_DO, TaskPriority.LOW, 0);
        Task deletedTask = new Task(aliceBoard, "Excluída", null, TaskStatus.TO_DO, TaskPriority.LOW, 1);
        deletedTask.markDeleted();

        taskRepository.saveAll(List.of(activeTask, deletedTask));

        mockMvc.perform(get("/api/boards/{boardId}", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.columns.TO_DO", hasSize(1)))
            .andExpect(jsonPath("$.columns.TO_DO[0].title").value("Ativa"));
    }

    @Test
    @DisplayName("Deve filtrar tarefas por status e prioridade")
    void shouldFilterTasksByStatusAndPriority() throws Exception {
        taskRepository.save(new Task(aliceBoard, "T1", null, TaskStatus.TO_DO, TaskPriority.LOW, 0));
        taskRepository.save(new Task(aliceBoard, "T2", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, 0));
        taskRepository.save(new Task(aliceBoard, "T3", null, TaskStatus.IN_PROGRESS, TaskPriority.URGENT, 1));

        // Filtro por status
        mockMvc.perform(get("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice")
                .param("status", "IN_PROGRESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));

        // Filtro combinado status + prioridade
        mockMvc.perform(get("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_alice")
                .param("status", "IN_PROGRESS")
                .param("priority", "HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("T2"));
    }

    @Test
    @DisplayName("Deve garantir isolamento: Bob não pode criar nem ler tarefas no quadro da Alice (retorna 404)")
    void shouldEnforceTenantIsolationBetweenUsers() throws Exception {
        TaskCreateRequestDTO request = new TaskCreateRequestDTO("Tentativa Invasão", null, TaskStatus.TO_DO, TaskPriority.HIGH, null);

        // Bob tentando criar tarefa no quadro da Alice
        mockMvc.perform(post("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_bob")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        // Bob tentando ver o quadro da Alice
        mockMvc.perform(get("/api/boards/{boardId}", aliceBoard.getId())
                .header("X-Mock-User", "clerk_bob"))
            .andExpect(status().isNotFound());

        // Bob tentando listar tarefas do quadro da Alice
        mockMvc.perform(get("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .header("X-Mock-User", "clerk_bob"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized para acessos sem cabeçalho de autenticação")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/boards/{boardId}", aliceBoard.getId()))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/boards/{boardId}/tasks", aliceBoard.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## 8. Considerações de Transição e Padrões Arquiteturais

1. **Substituição da Rota Legada `/api/tasks`:**
   - No modelo anterior (monolítico inicial de estudos), havia um endpoint `/api/tasks` sem vínculo com quadros.
   - Conforme a especificação SaaS Kanban (`SPEC.md` seções 3 e 4.2), tarefas existem estritamente no escopo de um `Board` (`/api/boards/{boardId}/tasks`).
   - O legado `TaskController` pode ser depreciado ou adaptado para redirecionar ou consultar o quadro ativo padrão do usuário.

2. **Integridade de Chaves e Performance:**
   - Os índices compostos `@Index(name = "idx_task_board_status", columnList = "board_id, status, deleted_at")` e `@Index(name = "idx_task_board_position", columnList = "board_id, status, position")` evitam full table scan ao montar a visualização Kanban e ao buscar a posição máxima de uma coluna.

3. **Garantia de 4 Colunas no Schema JSON:**
   - O front-end Kanban requer que a resposta JSON contenha as 4 chaves canônicas (`TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`) mesmo que alguma coluna não possua cartões no momento. `BoardKanbanViewDTO.fromDomain` assegura essa inicialização através de `LinkedHashMap` com listas vazias.
