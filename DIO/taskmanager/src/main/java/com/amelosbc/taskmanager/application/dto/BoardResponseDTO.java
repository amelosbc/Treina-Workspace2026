package com.amelosbc.taskmanager.application.dto;

import com.amelosbc.taskmanager.domain.Board;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO representing a Kanban Board.
 */
@Schema(description = "Representação detalhada de um Quadro Kanban")
public record BoardResponseDTO(
    @Schema(description = "ID único do quadro", example = "1")
    Long id,

    @Schema(description = "Nome do quadro", example = "Estudos DIO")
    String name,

    @Schema(description = "Descrição do quadro", example = "Acompanhamento das tarefas")
    String description,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última atualização")
    LocalDateTime updatedAt
) {
    public static BoardResponseDTO fromDomain(Board board) {
        return new BoardResponseDTO(
            board.getId(),
            board.getName(),
            board.getDescription(),
            board.getCreatedAt(),
            board.getUpdatedAt()
        );
    }
}
