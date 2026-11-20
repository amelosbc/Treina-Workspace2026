package com.amelosbc.taskmanager.infrastructure;

import com.amelosbc.taskmanager.application.BoardService;
import com.amelosbc.taskmanager.application.dto.BoardRequestDTO;
import com.amelosbc.taskmanager.application.dto.BoardResponseDTO;
import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@Tag(name = "Boards", description = "Endpoints para gerenciamento de quadros Kanban do usuário")
@SecurityRequirement(name = "DevMockUser")
@SecurityRequirement(name = "ClerkBearerAuth")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @Operation(summary = "Listar quadros ativos do usuário autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de quadros retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    public ResponseEntity<List<BoardResponseDTO>> listBoards(
            @AuthenticationPrincipal CurrentUserContext currentUser
    ) {
        return ResponseEntity.ok(boardService.getAllBoards(currentUser.userId()));
    }

    @Operation(summary = "Obter detalhes de um quadro ativo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quadro encontrado"),
        @ApiResponse(responseCode = "404", description = "Quadro não encontrado ou não pertence ao usuário"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> getBoardById(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUserContext currentUser
    ) {
        return ResponseEntity.ok(boardService.getBoardById(id, currentUser.userId()));
    }

    @Operation(summary = "Criar novo quadro Kanban")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Quadro criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos no payload"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @Valid @RequestBody BoardRequestDTO request,
            @AuthenticationPrincipal CurrentUserContext currentUser
    ) {
        BoardResponseDTO created = boardService.createBoard(request, currentUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Atualizar metadados de um quadro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quadro atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody BoardRequestDTO request,
            @AuthenticationPrincipal CurrentUserContext currentUser
    ) {
        return ResponseEntity.ok(boardService.updateBoard(id, request, currentUser.userId()));
    }

    @Operation(summary = "Excluir quadro (Soft Delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Quadro excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUserContext currentUser
    ) {
        boardService.deleteBoard(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
