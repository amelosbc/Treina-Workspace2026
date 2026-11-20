package com.amelosbc.taskmanager.infrastructure;

import com.amelosbc.taskmanager.application.dto.BoardRequestDTO;
import com.amelosbc.taskmanager.domain.Board;
import com.amelosbc.taskmanager.domain.BoardRepository;
import com.amelosbc.taskmanager.domain.Task;
import com.amelosbc.taskmanager.domain.TaskRepository;
import com.amelosbc.taskmanager.domain.User;
import com.amelosbc.taskmanager.domain.UserRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BoardControllerTest {

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

    private User userA;
    private User userB;
    private Board boardA;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(new User("clerk-user-a", "usera@dio.me", "User A"));
        userB = userRepository.save(new User("clerk-user-b", "userb@dio.me", "User B"));

        boardA = boardRepository.save(new Board(userA, "Quadro User A", "Descrição Quadro A"));
    }

    @Test
    @DisplayName("GET /api/boards deve retornar apenas os quadros ativos do usuário autenticado")
    void shouldListOnlyAuthenticatedUserBoards() throws Exception {
        // Cria um quadro para userB
        boardRepository.save(new Board(userB, "Quadro User B", "Descrição Quadro B"));

        mockMvc.perform(get("/api/boards")
                .header("X-Mock-User", "clerk-user-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Quadro User A")));
    }

    @Test
    @DisplayName("Isolamento: Usuário B não pode acessar o quadro do Usuário A (deve retornar 404)")
    void shouldReturn404WhenUserBAccessesUserABoard() throws Exception {
        mockMvc.perform(get("/api/boards/" + boardA.getId())
                .header("X-Mock-User", "clerk-user-b"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Isolamento: Usuário B não pode alterar o quadro do Usuário A (deve retornar 404)")
    void shouldReturn404WhenUserBUpdatesUserABoard() throws Exception {
        BoardRequestDTO updatePayload = new BoardRequestDTO("Tentativa Hack", "Invasao");

        mockMvc.perform(put("/api/boards/" + boardA.getId())
                .header("X-Mock-User", "clerk-user-b")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isNotFound());

        // Confirma que não houve alteração no banco
        Board original = boardRepository.findById(boardA.getId()).orElseThrow();
        assertEquals("Quadro User A", original.getName());
    }

    @Test
    @DisplayName("Isolamento: Usuário B não pode deletar o quadro do Usuário A (deve retornar 404)")
    void shouldReturn404WhenUserBDeletesUserABoard() throws Exception {
        mockMvc.perform(delete("/api/boards/" + boardA.getId())
                .header("X-Mock-User", "clerk-user-b"))
                .andExpect(status().isNotFound());

        Board original = boardRepository.findById(boardA.getId()).orElseThrow();
        assertNull(original.getDeletedAt());
    }

    @Test
    @DisplayName("DELETE /api/boards/{id} deve realizar Soft Delete e sumir da listagem ativa")
    void shouldPerformSoftDeleteSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/boards/" + boardA.getId())
                .header("X-Mock-User", "clerk-user-a"))
                .andExpect(status().isNoContent());

        // Chamada direta pelo ID agora retorna 404
        mockMvc.perform(get("/api/boards/" + boardA.getId())
                .header("X-Mock-User", "clerk-user-a"))
                .andExpect(status().isNotFound());

        // Listagem agora retorna vazio
        mockMvc.perform(get("/api/boards")
                .header("X-Mock-User", "clerk-user-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Registro ainda existe fisicamente no banco com deleted_at preenchido
        Board deletedEntity = boardRepository.findById(boardA.getId()).orElseThrow();
        assertNotNull(deletedEntity.getDeletedAt());
    }

    @Test
    @DisplayName("POST /api/boards com dados válidos deve criar e retornar 201 Created")
    void shouldCreateBoardSuccessfully() throws Exception {
        BoardRequestDTO request = new BoardRequestDTO("Novo Quadro Estudos", "Rotina Semanal");

        mockMvc.perform(post("/api/boards")
                .header("X-Mock-User", "clerk-user-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Novo Quadro Estudos")))
                .andExpect(jsonPath("$.description", is("Rotina Semanal")));
    }

    @Test
    @DisplayName("POST /api/boards com nome vazio deve retornar 400 Bad Request")
    void shouldRejectEmptyBoardName() throws Exception {
        BoardRequestDTO request = new BoardRequestDTO("", "Sem nome");

        mockMvc.perform(post("/api/boards")
                .header("X-Mock-User", "clerk-user-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Onboarding: Primeiro acesso de usuário deve criar 'Meu Primeiro Quadro' com 3 tarefas")
    void shouldTriggerOnboardingForNewUser() throws Exception {
        String newClerkUser = "new-onboarding-user";

        mockMvc.perform(get("/api/boards")
                .header("X-Mock-User", newClerkUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Meu Primeiro Quadro")));

        // Verifica as tarefas no banco criadas para o quadro de onboarding
        Board onboardingBoard = boardRepository.findAllByUserIdAndDeletedAtIsNull(
                userRepository.findByClerkUserId(newClerkUser).orElseThrow().getId()
        ).getFirst();

        List<Task> tasks = taskRepository.findAll();
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Bem-vindo ao seu Kanban!")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Arraste os cartões entre as colunas")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Conclua suas metas dos estudos DIO!")));
    }
}
