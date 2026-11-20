package com.amelosbc.taskmanager.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Board} entities.
 *
 * <p>Includes tenant isolation and soft delete filtering query methods.</p>
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * Retorna todos os quadros ativos de um usuário específico.
     */
    List<Board> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * Busca um quadro ativo por ID e usuário (garante tenant isolation).
     */
    Optional<Board> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /**
     * Verifica se existe pelo menos um quadro ativo pertencente a um usuário.
     */
    boolean existsByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * Verifica se existe um quadro ativo com ID específico pertencente a um usuário.
     */
    boolean existsByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /**
     * Busca um quadro ativo apenas pelo ID.
     */
    Optional<Board> findByIdAndDeletedAtIsNull(Long id);
}
