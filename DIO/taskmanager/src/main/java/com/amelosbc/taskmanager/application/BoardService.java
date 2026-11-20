package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.application.dto.BoardRequestDTO;
import com.amelosbc.taskmanager.application.dto.BoardResponseDTO;
import com.amelosbc.taskmanager.domain.Board;
import com.amelosbc.taskmanager.domain.BoardRepository;
import com.amelosbc.taskmanager.domain.User;
import com.amelosbc.taskmanager.domain.UserRepository;
import com.amelosbc.taskmanager.infrastructure.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer responsible for Board business logic, tenant isolation, and soft delete.
 */
@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public BoardService(BoardRepository boardRepository, UserRepository userRepository) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retorna todos os quadros ativos do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<BoardResponseDTO> getAllBoards(Long userId) {
        return boardRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(BoardResponseDTO::fromDomain)
                .toList();
    }

    /**
     * Retorna os detalhes de um quadro ativo pertencente ao usuário.
     */
    @Transactional(readOnly = true)
    public BoardResponseDTO getBoardById(Long id, Long userId) {
        Board board = boardRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + id));
        return BoardResponseDTO.fromDomain(board);
    }

    /**
     * Cria um novo quadro associado ao usuário.
     */
    @Transactional
    public BoardResponseDTO createBoard(BoardRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Board board = new Board(user, request.name(), request.description());
        Board saved = boardRepository.save(board);
        return BoardResponseDTO.fromDomain(saved);
    }

    /**
     * Atualiza metadados (nome, descrição) de um quadro existente.
     */
    @Transactional
    public BoardResponseDTO updateBoard(Long id, BoardRequestDTO request, Long userId) {
        Board board = boardRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + id));

        board.setName(request.name());
        board.setDescription(request.description());

        Board updated = boardRepository.save(board);
        return BoardResponseDTO.fromDomain(updated);
    }

    /**
     * Realiza soft delete do quadro.
     */
    @Transactional
    public void deleteBoard(Long id, Long userId) {
        Board board = boardRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + id));

        board.markDeleted();
        boardRepository.save(board);
    }
}
