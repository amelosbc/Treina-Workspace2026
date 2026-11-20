package com.amelosbc.taskmanager.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a Kanban Board.
 */
@Schema(description = "Dados para criação ou atualização de um Quadro Kanban")
public record BoardRequestDTO(

    @Schema(description = "Nome do quadro", example = "Estudos DIO - Backend Java")
    @NotBlank(message = "O nome do quadro é obrigatório")
    @Size(max = 150, message = "O nome do quadro deve ter no máximo 150 caracteres")
    String name,

    @Schema(description = "Descrição detalhada do propósito do quadro", example = "Quadro de acompanhamento do bootcamp DIO")
    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
    String description
) {
}
